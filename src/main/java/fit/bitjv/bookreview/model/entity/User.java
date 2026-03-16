package fit.bitjv.bookreview.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


@Getter
@Entity
@NoArgsConstructor
@Table(name = "\"user\"")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long id;

    @Setter
    @Column(unique = true)
    private String username;

    @Setter
    @Column(unique = true)
    private String email;

    @Setter
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Complaint> complaints = new HashSet<>();

    @Setter
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}