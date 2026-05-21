# Testing de la API key de Maileroo

> **Plan activo**: Free — 3.000 emails/mes · 30 emails/hora · 2 semanas retención logs

---

## Requisitos previos

| Variable | Valor | Dónde obtenerla |
|----------|-------|-----------------|
| `MAILEROO_API_KEY` | `mrk_xxxxx...` | [maileroo.com → API Keys](https://maileroo.com) |
| `MAIL_FROM` | `noreply@tudominio.com` | Dominio verificado en Maileroo (ver [Checklist DKIM/SPF](#checklist-dkimspf)) |

---

## Comandos de test

### Linux / Mac — email individual

```bash
curl -X POST https://api.maileroo.com/send \
  -H "X-API-Key: $MAILEROO_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "from": "noreply@atalayas.com",
    "to": "tu@email.com",
    "subject": "Test Maileroo — individual",
    "html": "<p>✅ API key funcionando correctamente.</p>"
  }'
```

Respuesta esperada:
```json
{"id":"msg_xxxxxxxx","status":"queued"}
```

---

### Windows — PowerShell

```powershell
$body = @{
    from    = "noreply@atalayas.com"
    to      = "tu@email.com"
    subject = "Test Maileroo — individual"
    html    = "<p>✅ API key funcionando correctamente.</p>"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "https://api.maileroo.com/send" `
  -Method POST `
  -Headers @{
      "X-API-Key"    = $env:MAILEROO_API_KEY
      "Content-Type" = "application/json"
  } `
  -Body $body
```

---

### Test BCC — envío masivo eventos

```bash
curl -X POST https://api.maileroo.com/send \
  -H "X-API-Key: $MAILEROO_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "from": "noreply@atalayas.com",
    "to": "noreply@atalayas.com",
    "bcc": "destinatario1@email.com,destinatario2@email.com",
    "subject": "Test BCC — evento masivo",
    "html": "<p>🎉 Test de envío BCC para notificaciones de eventos.</p>"
  }'
```

> **Nota**: el campo `to` apunta al remitente para que los destinatarios en BCC
> no se vean entre sí. Cada usuario recibe el mismo email sin exponer las otras direcciones.

---

## Tabla de respuestas

| HTTP | Significado | Acción |
|------|-------------|--------|
| `200` | ✅ Email enviado / encolado | Revisar bandeja de entrada (y carpeta spam) |
| `401` | ❌ API key inválida o ausente | Verificar `MAILEROO_API_KEY` en maileroo.com → API Keys |
| `422` | ❌ Dominio no verificado o body malformado | Verificar dominio; revisar que `MAIL_FROM` esté en el dominio verificado |
| `429` | ❌ Rate limit alcanzado (30/hora) | Esperar a la siguiente ventana horaria; considerar upgrade de plan |
| `5xx` | ❌ Error servidor Maileroo | Reintentar; consultar [status.maileroo.com](https://status.maileroo.com) |

---

## Configuración en Render

1. Panel Render → servicio → **Environment**
2. Añadir:

```
MAILEROO_API_KEY   = mrk_xxxxxxxxxxxxxxxxxxxx
MAIL_FROM          = noreply@atalayas.com
```

3. Opcionales (tienen valor por defecto):

```
MAILEROO_API_URL         = https://api.maileroo.com/send   # default
MAIL_MAX_BULK_RECIPIENTS = 25                              # cap BCC plan Free
```

4. Hacer **Save Changes** — el servicio se redesplegará automáticamente.

---

## Verificación en logs del servidor

Buscar entradas `DEBUG` / `ERROR` del logger `MailerooClient`:

```
# Éxito — email individual
DEBUG c.a.b.communication.client.MailerooClient : [MailerooClient] Email enviado a user@empresa.com — HTTP 200

# Éxito — email BCC masivo (eventos)
DEBUG c.a.b.communication.client.MailerooClient : [MailerooClient] BCC enviado a 18 destinatarios — HTTP 200

# Error de autenticación
ERROR c.a.b.communication.client.MailerooClient : [MailerooClient] Error HTTP 401 al enviar a user@empresa.com: ...

# Cap de destinatarios alcanzado (plan Free)
WARN  c.a.b.communication.service.EmailService  : [EmailService] NuevoEvento[Jornada Innovación] — lista de 40 destinatarios truncada a 25 (límite plan Maileroo Free 30/h)
```

---

## Checklist DKIM/SPF

Pasos en el panel de Maileroo **antes** de enviar en producción:

- [ ] Añadir dominio en **maileroo.com → Sending Domains → Add Domain**
- [ ] Añadir registro **SPF** en DNS del dominio:
  ```
  TXT  @   v=spf1 include:maileroo.com ~all
  ```
- [ ] Añadir registro **DKIM** en DNS (CNAME proporcionado por Maileroo):
  ```
  CNAME  maileroo._domainkey   maileroo._domainkey.maileroo.com
  ```
- [ ] Verificar en el panel que ambos registros aparecen con ✅ verde
- [ ] Confirmar que `MAIL_FROM` usa el dominio verificado (`@atalayas.com`, no `@gmail.com`)
- [ ] Enviar email de test y comprobar que pasa los filtros de spam

---

## Límites del plan Free y cuota de eventos

| Concepto | Valor | Impacto en eventos |
|----------|-------|--------------------|
| Emails/mes | 3.000 | ~120 notificaciones de evento con 25 usuarios |
| Emails/hora | 30 | Cap BCC configurado en 25 (`MAIL_MAX_BULK_RECIPIENTS`) |
| Emails individuales reservados | ~5/hora | Aprobaciones, rechazos, bienvenidas, resets |

> Si el número de usuarios supera los 25 activos, los destinatarios de cada envío
> masivo se truncan a 25 y se emite un log `WARN`. Subir el cap requiere upgrade
> del plan Maileroo o ajustar `MAIL_MAX_BULK_RECIPIENTS` con precaución.

---

## Diagnóstico rápido

```bash
# ¿Está configurada la clave?
echo $MAILEROO_API_KEY

# ¿Responde la API?
curl -s -o /dev/null -w "%{http_code}" \
  -X POST https://api.maileroo.com/send \
  -H "X-API-Key: $MAILEROO_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"from":"noreply@atalayas.com","to":"noreply@atalayas.com","subject":"ping","html":"."}'
# Esperado: 200 (o 422 si el dominio aún no está verificado — la clave es válida)
```



