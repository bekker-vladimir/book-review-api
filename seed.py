import argparse
import random
import json
import requests
from faker import Faker
from rich.console import Console
from rich.progress import Progress, SpinnerColumn, BarColumn, TextColumn

console = Console()
fake = Faker()

BASE_URL = "http://localhost:8080"
CREDENTIALS_FILE = "test_credentials.txt"
SEEDED_IDS_FILE = "seeded_ids.json"  # tracks created IDs for rollback

ADMIN_USERNAME = "test"
ADMIN_PASSWORD = "123456"

GENRES = ['Fiction', 'Non-Fiction', 'Science Fiction', 'Fantasy', 'Mystery',
          'Romance', 'Thriller', 'Horror', 'Classic']


# ─── ID tracking (for rollback) ────────────────────────────────────────────────

def load_seeded_ids():
    try:
        with open(SEEDED_IDS_FILE, "r") as f:
            return json.load(f)
    except FileNotFoundError:
        return {"users": [], "books": []}


def save_seeded_ids(data):
    with open(SEEDED_IDS_FILE, "w") as f:
        json.dump(data, f, indent=2)


# ─── Auth helpers ───────────────────────────────────────────────────────────────

def login(username, password):
    try:
        res = requests.post(f"{BASE_URL}/auth/login", json={
            "username": username,
            "password": password
        })
        if res.status_code == 200:
            return res.text.strip().strip('"')
        return None
    except Exception:
        return None


def admin_token():
    token = login(ADMIN_USERNAME, ADMIN_PASSWORD)
    if not token:
        console.print("[red]Failed to login as admin. Check ADMIN_USERNAME/ADMIN_PASSWORD.[/red]")
    return token


# ─── Users ──────────────────────────────────────────────────────────────────────

def generate_users(n):
    console.print(f"[bold cyan]Generating {n} users...[/bold cyan]")
    users = []
    seeded = load_seeded_ids()

    with Progress(SpinnerColumn(), TextColumn("[progress.description]{task.description}"),
                  BarColumn(), TextColumn("[progress.percentage]{task.percentage:>3.0f}%")) as progress:
        task = progress.add_task("[cyan]Registering users...", total=n)

        with open(CREDENTIALS_FILE, "a") as f:
            for _ in range(n):
                username = fake.user_name() + str(random.randint(100, 9999))
                password = fake.password()
                email = fake.email()
                try:
                    res = requests.post(f"{BASE_URL}/auth/register", json={
                        "username": username,
                        "password": password,
                        "email": email
                    })
                    if res.status_code == 200:
                        f.write(f"{username}:{password}\n")
                        users.append({"username": username, "password": password})
                        seeded["users"].append(username)
                    else:
                        console.print(f"[red]Failed {username}: {res.status_code}[/red]")
                except Exception as e:
                    console.print(f"[red]Error: {e}[/red]")
                progress.advance(task)

    save_seeded_ids(seeded)
    console.print(f"[bold green]Registered {len(users)} users.[/bold green]")
    return users


def get_users_from_file():
    users = []
    try:
        with open(CREDENTIALS_FILE, "r") as f:
            for line in f:
                parts = line.strip().split(":", 1)
                if len(parts) == 2:
                    users.append({"username": parts[0], "password": parts[1]})
    except FileNotFoundError:
        pass
    return users


# ─── Books ──────────────────────────────────────────────────────────────────────

def fetch_books_from_openlibrary(n):
    console.print(f"[bold cyan]Fetching {n} books from OpenLibrary...[/bold cyan]")
    try:
        res = requests.get(
            f"https://openlibrary.org/search.json?q=fiction&limit={n}",
            timeout=15
        )
        if res.status_code == 200:
            return res.json().get("docs", [])
        console.print(f"[red]OpenLibrary error: {res.status_code}[/red]")
    except requests.exceptions.Timeout:
        console.print("[red]OpenLibrary request timed out after 15s.[/red]")
    except Exception as e:
        console.print(f"[red]Error fetching books: {e}[/red]")
    return []


def fetch_cover(ol_book):
    """
    Download cover from OpenLibrary Covers API.
    Tries cover_i (numeric ID) first, then first ISBN.
    Returns (bytes, extension) or (None, None) if unavailable.
    """
    cover_id = ol_book.get("cover_i")
    if cover_id:
        url = f"https://covers.openlibrary.org/b/id/{cover_id}-L.jpg"
        try:
            r = requests.get(url, timeout=10)
            # OpenLibrary returns a 1x1 gif placeholder for missing covers
            if r.status_code == 200 and len(r.content) > 1000:
                return r.content, ".jpg"
        except Exception:
            pass

    isbns = ol_book.get("isbn", [])
    if isbns:
        url = f"https://covers.openlibrary.org/b/isbn/{isbns[0]}-L.jpg"
        try:
            r = requests.get(url, timeout=10)
            if r.status_code == 200 and len(r.content) > 1000:
                return r.content, ".jpg"
        except Exception:
            pass

    return None, None


def upload_cover(book_id, cover_bytes, ext, token):
    try:
        # Do NOT set Content-Type manually — requests sets it with the correct boundary
        files = {"file": (f"cover{ext}", cover_bytes, "image/jpeg")}
        headers = {"Authorization": f"Bearer {token}"}
        res = requests.post(f"{BASE_URL}/books/{book_id}/cover",
                            files=files, headers=headers)
        return res.status_code in (200, 201)
    except Exception:
        return False


def load_books(n, users, with_covers=True):
    if not users:
        console.print("[red]No users available.[/red]")
        return []

    books_data = fetch_books_from_openlibrary(n)
    if not books_data:
        return []

    seeded = load_seeded_ids()
    token = admin_token()  # used for cover upload (requires ADMIN/MODERATOR)
    created_book_ids = []

    console.print(f"[bold cyan]Submitting {len(books_data)} books...[/bold cyan]")

    with Progress(SpinnerColumn(), TextColumn("[progress.description]{task.description}"),
                  BarColumn(), TextColumn("[progress.percentage]{task.percentage:>3.0f}%")) as progress:
        task = progress.add_task("[cyan]Creating books...", total=len(books_data))

        for book in books_data:
            user = random.choice(users)
            user_token = login(user["username"], user["password"])
            if not user_token:
                progress.advance(task)
                continue

            title = book.get("title", "Unknown Title")[:255]
            authors = book.get("author_name", ["Unknown Author"])
            subjects = book.get("subject", [])
            genre = next((s for s in subjects if s in GENRES), random.choice(GENRES))
            publish_year = str(book.get("first_publish_year", ""))

            payload = {
                "title": title,
                "genre": genre,
                "description": f"{title} — {', '.join(authors[:2])}. {fake.sentence(nb_words=12)}",
                "publicationDate": publish_year,
                "authorNames": authors[:5],
            }

            try:
                res = requests.post(
                    f"{BASE_URL}/books", json=payload,
                    headers={"Authorization": f"Bearer {user_token}",
                             "Content-Type": "application/json"}
                )
                if res.status_code in (200, 201):
                    book_id = res.json().get("id")
                    created_book_ids.append(book_id)

                    if with_covers and book_id and token:
                        cover_bytes, ext = fetch_cover(book)
                        if cover_bytes:
                            upload_cover(book_id, cover_bytes, ext, token)
                else:
                    console.print(f"[red]Failed '{title}': {res.status_code} — {res.text}[/red]")
            except Exception as e:
                console.print(f"[red]Error '{title}': {e}[/red]")

            progress.advance(task)

    seeded["books"].extend(created_book_ids)
    save_seeded_ids(seeded)
    console.print(f"[bold green]Created {len(created_book_ids)} books.[/bold green]")
    return created_book_ids


# ─── Approve ────────────────────────────────────────────────────────────────────

def approve_books():
    console.print("[bold cyan]Approving pending books...[/bold cyan]")
    token = admin_token()
    if not token:
        return

    headers = {"Authorization": f"Bearer {token}"}

    res = requests.get(f"{BASE_URL}/books/pending", headers=headers)
    if res.status_code != 200:
        console.print(f"[red]Failed to fetch pending books: {res.status_code} — {res.text}[/red]")
        return

    pending = res.json()
    if not pending:
        console.print("[yellow]No pending books.[/yellow]")
        return

    # Keep last 2 pending to demonstrate the admin panel
    to_approve = pending[:-2] if len(pending) > 2 else pending

    with Progress(SpinnerColumn(), TextColumn("[progress.description]{task.description}"),
                  BarColumn(), TextColumn("[progress.percentage]{task.percentage:>3.0f}%")) as progress:
        task = progress.add_task("[cyan]Approving...", total=len(to_approve))

        for book in to_approve:
            book_id = book.get("id")
            if not book_id:
                progress.advance(task)
                continue
            r = requests.patch(
                f"{BASE_URL}/books/{book_id}/status",
                json={"status": "APPROVED"},
                headers=headers
            )
            if r.status_code not in (200, 201):
                console.print(f"[red]Failed to approve {book_id}: {r.status_code}[/red]")
            progress.advance(task)

    console.print(f"[bold green]Approved {len(to_approve)} books "
                  f"({len(pending) - len(to_approve)} left pending).[/bold green]")


# ─── Reviews ────────────────────────────────────────────────────────────────────

def generate_reviews(users, num_reviews_per_book=3):
    console.print("[bold cyan]Generating reviews...[/bold cyan]")
    if not users:
        console.print("[red]No users.[/red]")
        return

    res = requests.get(f"{BASE_URL}/books")
    if res.status_code != 200:
        console.print(f"[red]Failed to fetch books: {res.status_code}[/red]")
        return

    data = res.json()
    books = data.get("content", [])
    if not books:
        console.print("[yellow]No approved books found.[/yellow]")
        return

    total = len(books) * num_reviews_per_book

    with Progress(SpinnerColumn(), TextColumn("[progress.description]{task.description}"),
                  BarColumn(), TextColumn("[progress.percentage]{task.percentage:>3.0f}%")) as progress:
        task = progress.add_task("[cyan]Writing reviews...", total=total)

        for book in books:
            book_id = book.get("id")
            reviewers = random.sample(users, min(num_reviews_per_book, len(users)))

            for user in reviewers:
                user_token = login(user["username"], user["password"])
                if user_token:
                    try:
                        requests.post(
                            f"{BASE_URL}/books/{book_id}/reviews",
                            json={
                                "rating": random.randint(1, 5),
                                "comment": fake.paragraph(nb_sentences=random.randint(1, 4))
                            },
                            headers={"Authorization": f"Bearer {user_token}"}
                        )
                    except Exception:
                        pass
                progress.advance(task)

    console.print("[bold green]Reviews done.[/bold green]")


# ─── Rollback ───────────────────────────────────────────────────────────────────

def rollback():
    """
    Delete all books and users created by this script.
    Books are deleted via DELETE /books/{id} (reviews/complaints cascade on the DB side).
    Users have no REST endpoint for deletion — SQL is printed for manual cleanup.
    """
    console.print("[bold red]Rolling back seeded data...[/bold red]")
    seeded = load_seeded_ids()
    token = admin_token()

    if not token:
        console.print("[red]Cannot rollback without admin token.[/red]")
        return

    headers = {"Authorization": f"Bearer {token}"}

    # Delete books
    book_ids = seeded.get("books", [])
    if book_ids:
        console.print(f"[cyan]Deleting {len(book_ids)} books...[/cyan]")
        ok = 0
        for book_id in book_ids:
            try:
                r = requests.delete(f"{BASE_URL}/books/{book_id}", headers=headers)
                if r.status_code in (200, 204):
                    ok += 1
                else:
                    console.print(f"[yellow]Book {book_id}: {r.status_code}[/yellow]")
            except Exception as e:
                console.print(f"[red]Error deleting book {book_id}: {e}[/red]")
        console.print(f"[green]Deleted {ok}/{len(book_ids)} books.[/green]")
    else:
        console.print("[yellow]No books to delete.[/yellow]")

    # Users — no DELETE /users endpoint exists, emit SQL
    usernames = seeded.get("users", [])
    if usernames:
        names_sql = ", ".join(f"'{u}'" for u in usernames)
        console.print(f"\n[yellow]No DELETE /users endpoint — run this SQL to remove "
                      f"{len(usernames)} seeded users:[/yellow]")
        console.print(f'[bold white]DELETE FROM "user" WHERE username IN ({names_sql});[/bold white]\n')
    else:
        console.print("[yellow]No users to remove.[/yellow]")

    save_seeded_ids({"users": [], "books": []})
    open(CREDENTIALS_FILE, "w").close()
    console.print("[bold green]Rollback complete. Tracking files cleared.[/bold green]")


# ─── CLI ────────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Seed / rollback test data for Book Review API")
    parser.add_argument("--users",     type=int,            help="Number of users to generate",                default=0)
    parser.add_argument("--books",     type=int,            help="Number of books to fetch from OpenLibrary",  default=0)
    parser.add_argument("--approve",   action="store_true", help="Approve pending books as admin")
    parser.add_argument("--reviews",   action="store_true", help="Generate reviews for approved books")
    parser.add_argument("--no-covers", action="store_true", help="Skip cover download/upload when loading books")
    parser.add_argument("--rollback",  action="store_true", help="Delete all data created by this script")
    parser.add_argument("--all",       action="store_true", help="Full seed: users + books + approve + reviews")

    args = parser.parse_args()

    if args.rollback:
        rollback()
        return

    if args.all:
        users = generate_users(args.users if args.users > 0 else 10)
        load_books(args.books if args.books > 0 else 20, users, with_covers=not args.no_covers)
        approve_books()
        generate_reviews(users)
        return

    users = []
    if args.users > 0:
        users = generate_users(args.users)

    if args.books > 0:
        if not users:
            users = get_users_from_file()
        load_books(args.books, users, with_covers=not args.no_covers)

    if args.approve:
        approve_books()

    if args.reviews:
        if not users:
            users = get_users_from_file()
        generate_reviews(users)

    if not any([args.all, args.users, args.books, args.approve, args.reviews, args.rollback]):
        parser.print_help()


if __name__ == "__main__":
    main()