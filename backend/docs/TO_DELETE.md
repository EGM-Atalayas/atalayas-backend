# Archivos / Directorios a Eliminar

## Archivos

| Archivo | Motivo |
|---|---|
| `src/main/java/com/atalayas/backend/role/entity/Role.java` | Stub `@Deprecated` vacío. Reemplazado por `Rol.java`. Ningún archivo lo importa. |
| `src/main/java/com/atalayas/backend/role/service/RoleService.java` | Verificar si está vacío o si algún bean lo inyecta antes de eliminar. |

## Directorios vacíos

| Directorio | Motivo |
|---|---|
| `src/main/java/com/atalayas/backend/auth/mapper/` | Directorio vacío. Si no se planea añadir `AuthMapper`, eliminar. |

## Cómo eliminar (desde la raíz del proyecto)

```bash
rm src/main/java/com/atalayas/backend/role/entity/Role.java
rmdir src/main/java/com/atalayas/backend/auth/mapper
```

> **Antes de eliminar `Role.java`**, confirmar con:
> ```bash
> grep -r "import com.atalayas.backend.role.entity.Role" src/
> ```
> Si no hay resultados, es seguro eliminar.

