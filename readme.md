# Pedidos360 - LevelUp Gamer - Backend

Backend del sistema de pedidos para la tienda de hardware gamer LevelUp
Gamer, compuesto por dos microservicios Spring Boot protegidos con JWT
emitido por Microsoft Entra ID y expuestos a traves de AWS API Gateway.
Proyecto de la asignatura DSY1107 - Desarrollo Cloud Native I (Duoc UC).

Integrantes: Keiton Chaves - Matias Chavez
Seccion: DSY1107-004V
Docente: Edwin Sanchez Valderrama

---

## Arquitectura general

Flujo de autenticacion y comunicacion entre componentes:

1. El frontend Angular (con MSAL) inicia el login contra Microsoft Entra ID
   usando Authorization Code con PKCE.
2. Entra ID responde con un ID Token y un Access Token (JWT).
3. El frontend envia el Access Token en el header Authorization de cada
   peticion hacia AWS API Gateway.
4. API Gateway aplica CORS y, en las rutas protegidas, un autorizador JWT
   antes de reenviar la peticion al microservicio correspondiente.
5. Cada microservicio (ms-productos en el puerto 8080 y ms-pedidos en el
   puerto 8081) valida nuevamente el token (issuer, audience, firma,
   vigencia y roles) antes de procesar la solicitud.

Los usuarios no viven en la base de datos: la identidad, los roles y el
login los gestiona Entra ID. Cada microservicio se limita a validar el JWT
recibido y a leer el usuario desde el claim "preferred_username".

Este repositorio contiene ambos microservicios del backend:

| Microservicio | Responsabilidad | Puerto |
|---|---|---|
| msproductos | Catalogo de productos | 8080 |
| mspedidos | Gestion de pedidos | 8081 |

---

## Stack tecnico

| Tecnologia | Uso |
|---|---|
| Java 21 | Lenguaje base |
| Spring Boot | Framework de ambas APIs REST |
| Spring Data JPA | Persistencia sobre H2 (en memoria) |
| Spring Security - OAuth2 Resource Server | Validacion de JWT emitido por Entra ID |
| Maven Wrapper (mvnw) | Build sin depender de una instalacion local de Maven |
| Lombok | Reduccion de boilerplate en modelos y DTO |

---

## Patron de arquitectura

Ambos microservicios siguen el mismo patron de arquitectura en capas
(layered architecture), para mantener consistencia entre servicios y
facilitar que cualquier integrante del equipo entienda ambos con la misma
logica. La estructura de paquetes es la siguiente:

- config: seguridad (SecurityConfig con JWT), configuracion de CORS y de Swagger.
- controller: endpoints REST, capa de entrada HTTP.
- dto: objetos de transferencia de datos (request y response).
- model: entidades JPA.
- repository: interfaces Spring Data para el acceso a datos.
- service: logica de negocio.

Razones de esta separacion:

- El controller solo traduce HTTP a llamadas de negocio; no conoce reglas
  internas.
- El service concentra la logica (calculo de subtotales en pedidos,
  validacion de SKU unico en productos, cambios de estado) y es
  independiente del transporte HTTP.
- El repository abstrae el acceso a datos con Spring Data, permitiendo
  cambiar de motor de base de datos sin tocar el resto del codigo.
- Los DTO desacoplan el contrato publico de la API del modelo de
  persistencia interno, evitando exponer directamente las entidades JPA
  y posibles relaciones internas o campos sensibles.

ms-pedidos guarda una copia (snapshot) de cada producto (nombre y precio) al
momento de la compra, por lo que no depende de ms-productos en tiempo de
ejecucion: si el precio de un producto cambia despues, los pedidos ya
creados conservan el precio historico correcto.

---

## Seguridad: validacion de JWT con Microsoft Entra ID

Ambos microservicios actuan como OAuth2 Resource Server: no gestionan login
ni usuarios, solo validan el token recibido en cada request.

Configuracion base (application.properties):

```
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/<TENANT_ID>/v2.0
spring.security.oauth2.resourceserver.jwt.audiences=api://<CLIENT_ID_API>
```

SecurityConfig valida, en este orden, antes de dejar pasar cualquier
request protegida:

1. Firma del JWT contra las claves publicas (JWKS) publicadas por Entra ID.
2. Issuer, que el token venga del tenant correcto.
3. Audience, que el token este emitido especificamente para esta API.
4. Vigencia, que no este expirado.
5. Rol (APPROLE_admin o APPROLE_cliente) segun el endpoint solicitado.

### Rutas y roles en ms-productos

| Endpoint | Rol requerido | Codigo si falla |
|---|---|---|
| GET /productos | publico, sin token | no aplica |
| GET /productos?categoria= | publico, sin token | no aplica |
| GET /productos/{id} | publico, sin token | no aplica |
| POST /productos | admin | 401 sin token, 403 con rol incorrecto |
| DELETE /productos/{id} | admin | 401 sin token, 403 con rol incorrecto |

El catalogo es de acceso publico por regla de negocio de la tienda: se puede
navegar sin iniciar sesion. Esto se refleja tanto en SecurityConfig como en
API Gateway, donde la ruta /productos no tiene autorizador JWT asociado.

### Rutas y roles en ms-pedidos

| Endpoint | Rol requerido | Codigo si falla |
|---|---|---|
| GET /pedidos (todos) | admin | 401 o 403 |
| GET /pedidos?usuario= | usuario autenticado, sus propios pedidos | 401 |
| GET /pedidos/{id} | cliente o admin | 401 o 403 |
| POST /pedidos | cliente | 401 o 403 |
| PATCH /pedidos/{id}/estado | admin | 401 o 403 |

Se verifico con Postman que el backend devuelve 401 Unauthorized incluso al
acceder directamente a la IP publica de la instancia EC2, sin pasar por API
Gateway y sin token, confirmando que la validacion ocurre en el propio
microservicio y no unicamente en el gateway.

---

## Modelos de datos

Producto (msproductos): id, sku (unico), nombre, categoria, precio, stock.

Pedido (mspedidos): id, usuario (tomado del claim del JWT), fecha, estado
(CREADO, PAGADO, ENVIADO, ENTREGADO o CANCELADO), total, y una lista de
DetallePedido.

DetallePedido: productoId, nombreProducto (snapshot), precioUnitario
(snapshot), cantidad y subtotal (calculado por el service).

---

## Endpoints

### msproductos (/productos)

| Metodo | Ruta | Descripcion |
|---|---|---|
| GET | /productos | Lista el catalogo completo |
| GET | /productos?categoria=Audio | Filtra por categoria |
| GET | /productos/{id} | Detalle de un producto |
| POST | /productos | Crea un producto (admin) |
| DELETE | /productos/{id} | Elimina un producto (admin) |

Ejemplo de respuesta:

```
{ "id": 1, "sku": "NX-VK65-MAG", "nombre": "Valkyrie Apex Pro 65% Magnetic Keyboard",
  "categoria": "Teclados", "precio": 179990, "stock": 32 }
```

### mspedidos (/pedidos)

| Metodo | Ruta | Descripcion |
|---|---|---|
| GET | /pedidos | Lista todos los pedidos (admin) |
| GET | /pedidos?usuario=... | Pedidos de un usuario |
| GET | /pedidos/{id} | Detalle de un pedido |
| POST | /pedidos | Crea un pedido, calcula subtotales, total y estado CREADO |
| PATCH | /pedidos/{id}/estado | Cambia el estado, por ejemplo {"estado":"PAGADO"} |

Ejemplo de body de POST /pedidos:

```
{
  "usuario": "cliente@tenant.onmicrosoft.com",
  "detalles": [
    { "productoId": 5, "nombreProducto": "Kinetic Pro IEM", "precioUnitario": 169990, "cantidad": 2 }
  ]
}
```

---

## Como correr en local

Se necesitan dos terminales, una por microservicio.

Terminal 1, msproductos:

```
cd msproductos
mvnw clean spring-boot:run
```

API disponible en http://localhost:8080/productos
Consola H2 en http://localhost:8080/h2 (JDBC URL jdbc:h2:mem:pedidos360, usuario sa, sin clave)

Terminal 2, mspedidos:

```
cd mspedidos
mvnw clean spring-boot:run
```

API disponible en http://localhost:8081/pedidos
Consola H2 en http://localhost:8081/h2 (JDBC URL jdbc:h2:mem:pedidos360_pedidos, usuario sa, sin clave)

Se recomienda usar siempre "mvnw clean" antes de "spring-boot:run" si se
modifico application.properties o data.sql, ya que algunos IDE reutilizan
una compilacion en cache.

El catalogo inicial de msproductos (8 productos) se carga automaticamente
desde data.sql en cada arranque. mspedidos no depende de msproductos en
tiempo de ejecucion.

---

## Despliegue en AWS

1. EC2: una unica instancia (levelupback, Amazon Linux 2023) aloja ambos
   archivos .jar, cada uno corriendo como servicio systemd
   (msproductos.service en el puerto 8080, mspedidos.service en el puerto
   8081) en ejecucion continua.
2. API Gateway (HTTP API): rutas /productos/{proxy+} hacia EC2 puerto 8080 y
   /pedidos/{proxy+} hacia EC2 puerto 8081, con comodin {proxy+} y metodo ANY
   para cubrir todos los verbos y sub-paths sin declarar cada endpoint por
   separado. El autorizador JWT levelup-entra-jwt protege unicamente
   /pedidos, dejando /productos publico.
3. CORS: habilitado en el gateway para el origen del frontend
   (http://localhost:4200), con los headers y metodos necesarios
   (Authorization, Content-Type; GET, POST, PATCH, DELETE, OPTIONS).

---

## Pruebas realizadas

| Caso | Resultado esperado | Verificado |
|---|---|---|
| GET /productos sin token | 200 OK, acceso publico | Si |
| Token valido con rol correcto en ruta protegida | 200 OK | Si |
| Sin header Authorization en ruta protegida | 401 Unauthorized | Si |
| Token valido, rol incorrecto | 403 Forbidden | Si |
| Acceso directo a EC2 sin token, sin pasar por gateway | 401 Unauthorized | Si |

El token usado en las pruebas de Postman se extrajo de una sesion real del
frontend Angular (login con MSAL), inspeccionando el header Authorization
generado automaticamente por MsalInterceptor en las herramientas de
desarrollador del navegador. Tiene una vigencia aproximada de 90 minutos.