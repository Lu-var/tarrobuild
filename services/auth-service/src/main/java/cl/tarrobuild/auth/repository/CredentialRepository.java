package cl.tarrobuild.auth.repository;

import cl.tarrobuild.auth.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    public Optional<Credential> findByEmail(String email);
    public Boolean existsByEmail(String email);
}
