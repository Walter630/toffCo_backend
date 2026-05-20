#!/bin/bash

# Verifica se você digitou o nome do módulo
if [ -z "$1" ]; then
  echo "⚠️  Uso correto: ./gerar.sh NomeDoModulo"
  echo "💡 Exemplo: ./gerar.sh Produto"
  exit 1
fi

MODULO=$1
# Converte o nome para minúsculo para usar no nome das pastas
PACOTE=$(echo "$MODULO" | tr '[:upper:]' '[:lower:]')

# === MUDE AQUI PARA O SEU PACOTE BASE ===
PACOTE_BASE_JAVA="com.site.toffCo"
PACOTE_BASE_DIR="src/main/java/com/site/toffCo/module"
# ========================================

DIR_MODULO="$PACOTE_BASE_DIR/$PACOTE"

echo "🚀 Gerando módulo: $MODULO..."

# 1. Cria todas as pastas de uma vez
mkdir -p "$DIR_MODULO"/{controller,service,repository,entity,dto,mapper}

# 2. Cria a Entity
cat <<EOF > "$DIR_MODULO/entity/${MODULO}.java"
package $PACOTE_BASE_JAVA.$PACOTE.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tb_${PACOTE}")
public class ${MODULO} {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
}
EOF

# 3. Cria o Repository
cat <<EOF > "$DIR_MODULO/repository/${MODULO}Repository.java"
package $PACOTE_BASE_JAVA.$PACOTE.repository;

import $PACOTE_BASE_JAVA.$PACOTE.entity.${MODULO};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface ${MODULO}Repository extends JpaRepository<${MODULO}, UUID> {
}
EOF

# 4. Cria o Service
cat <<EOF > "$DIR_MODULO/service/${MODULO}Service.java"
package $PACOTE_BASE_JAVA.$PACOTE.service;

import $PACOTE_BASE_JAVA.$PACOTE.repository.${MODULO}Repository;
import org.springframework.stereotype.Service;

@Service
public class ${MODULO}Service {

    private final ${MODULO}Repository repository;

    public ${MODULO}Service(${MODULO}Repository repository) {
        this.repository = repository;
    }
}
EOF

# 5. Cria o Controller
cat <<EOF > "$DIR_MODULO/controller/${MODULO}Controller.java"
package $PACOTE_BASE_JAVA.$PACOTE.controller;

import $PACOTE_BASE_JAVA.$PACOTE.service.${MODULO}Service;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/${PACOTE}s")
public class ${MODULO}Controller {

    private final ${MODULO}Service service;

    public ${MODULO}Controller(${MODULO}Service service) {
        this.service = service;
    }
}
EOF

# 6. Cria o DTO (Record do Java 21/25)
cat <<EOF > "$DIR_MODULO/dto/${MODULO}ResponseDTO.java"
package $PACOTE_BASE_JAVA.$PACOTE.dto;
import java.util.UUID;

public record ${MODULO}ResponseDTO(
    UUID id
) {}
EOF

# 7. Cria o Mapper (MapStruct)
cat <<EOF > "$DIR_MODULO/mapper/${MODULO}Mapper.java"
package $PACOTE_BASE_JAVA.$PACOTE.mapper;

import $PACOTE_BASE_JAVA.$PACOTE.entity.${MODULO};
import $PACOTE_BASE_JAVA.$PACOTE.dto.${MODULO}ResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class ${MODULO}Mapper {
    public abstract ${MODULO}ResponseDTO toDto(${MODULO} entity);
}
EOF

echo "✅ Módulo $MODULO criado com sucesso com Entity, Controller, Service, Repository, DTO e Mapper!"