package it.polito.ai.backend.repositories;

import it.polito.ai.backend.entities.Teacher;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, String> {

    /*
     * Loads an entity with lazy property loaded from a database
     */
    @EntityGraph(attributePaths={"profilePicture"})
    Teacher findWithPropertyPictureAttachedById(String id);
}
