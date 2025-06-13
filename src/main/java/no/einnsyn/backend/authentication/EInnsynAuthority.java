package no.einnsyn.backend.authentication;

import java.util.Objects;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
public class EInnsynAuthority implements GrantedAuthority {

  // Entity name, Bruker or Enhet
  public final String entity;

  // eInnsyn ID for the entity
  public final String id;

  // Read, Write
  public final String access;

  public EInnsynAuthority(String entity, String id, String access) {
    this.entity = entity;
    this.id = id;
    this.access = access;
  }

  @Override
  public String getAuthority() {
    return "ENTITY_" + entity.toUpperCase() + "_" + access.toUpperCase() + "_" + id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof EInnsynAuthority eInnsynAuthority
        && entity.equals(eInnsynAuthority.entity)
        && id.equals(eInnsynAuthority.id)
        && access.equals(eInnsynAuthority.access)) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(entity, id, access);
  }

  @Override
  public String toString() {
    return getAuthority();
  }
}
