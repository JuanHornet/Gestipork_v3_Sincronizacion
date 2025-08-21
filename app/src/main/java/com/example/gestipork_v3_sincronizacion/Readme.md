# 🐷 GestiPork v3 - Sincronización

Aplicación móvil para la gestión de explotaciones porcinas en extensivo.  
Basada en **Android Studio (Java)** con **SQLite offline** y sincronización en la nube mediante **Supabase**.

---

## 📌 Convenciones de claves y nombres

- Todas las tablas tienen como **PK** un `id` (UUID).
- Las **FK** siguen la convención:
    - `id_explotacion` → referencia a `explotaciones.id`
    - `id_lote` → referencia a `lotes.id`
- Campos comunes en todas las tablas:
    - `fecha_actualizacion` → texto ISO (última modificación).
    - `sincronizado` → `0` (pendiente) / `1` (sincronizado).
    - `eliminado` → `0` (activo) / `1` (borrado lógico).
    - `fecha_eliminado` → fecha ISO del borrado lógico.

---

## 🔗 Relaciones entre entidades

### Relaciones 1:1
Cada **lote** tiene vinculados:
- 1 paridera
- 1 cubrición
- 1 itaca

👉 Se crean **automáticamente al crear un lote**.

### Relaciones 1:N
Cada **lote** puede tener múltiples:
- Acciones
- Salidas
- Bajas
- Notas
- Pesos
- Conteos

---

## 🗂 Estructura del proyecto


---

## 🔄 Estrategia de sincronización

1. **Inserción local** → se guarda en SQLite con `sincronizado=0`.
2. **WorkManager** sincroniza en segundo plano cada X minutos:
    - Envía registros `sincronizado=0` a Supabase.
    - Descarga registros modificados en Supabase y actualiza SQLite.
3. **Eliminación lógica**:
    - Se marca con `eliminado=1` y `fecha_eliminado`.
    - No se borra en Supabase (excepto acciones → borrado físico).
    - Si no hay conexión → se guarda en `eliminaciones_pendientes`.

---

## 🧮 Reglas de negocio clave

- `pesoMedio` se calcula en el **servicio** al insertar (no en el repo).
- Cada creación de lote dispara automáticamente la creación de:
    - Una fila en `parideras`
    - Una fila en `cubriciones`
    - Una fila en `itaca`
- Listados principales siempre filtrados por:
    - `id_explotacion` del usuario logado
    - `eliminado=0`

---

## ✅ Checklist de desarrollo

- [x] Migrar claves a UUID
- [x] Tablas base (`usuarios`, `explotaciones`, `lotes`)
- [x] Relaciones 1:1 (paridera, cubrición, itaca)
- [x] Relaciones 1:N (acciones, salidas, notas, pesos, contar, bajas)
- [ ] Repositorios remotos (Supabase)
- [ ] Sincronizadores (WorkManager)
- [ ] Pantallas principales (Dashboard, Lotes, Explotaciones)
- [ ] Test de integración

---

## 📅 Próximos pasos

- Implementar sincronización completa en `Notas`, `Pesos` y `Conteo`.
- Crear **UI mínima** para probar CRUD + sincronización.
- Añadir **logs de depuración** para WorkManager.

---

## 👨‍💻 Autor

Proyecto desarrollado por **Juan Lucas** como parte de la evolución de **GestiPork**.  
Ganadería extensiva + Desarrollo de software 📲🐖
