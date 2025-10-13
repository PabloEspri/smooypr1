from fastapi import FastAPI, HTTPException, Path, Query
from pydantic import BaseModel
import mysql.connector
from mysql.connector import Error
from typing import Optional, List, Dict, Any

# Conexión a la base de datos
def conectar_db():
    try:
        conexion = mysql.connector.connect(
            host="127.0.0.1",
            user="root",  # Cambia esto si usas otro usuario
            password="root",  # Cambia esto si usas otra contraseña
            database="smooydb",  # Asegúrate de que la base de datos es correcta
            port=3306
        )
        if conexion.is_connected():
            return conexion
    except Error as e:
        print(f"Error al conectar a la base de datos: {e}")
        return None

app = FastAPI()

# Definimos un modelo de datos para la solicitud de login
class LoginRequest(BaseModel):
    usuario: str
    contraseña: str

# Definimos un modelo de datos para el proceso
class Proceso(BaseModel):
    tipo_proceso: str
    descripcion: str
    frecuencia: str
    horario: str
    fecha_inicio: str
    fecha_fin: str
    estado: str
    establecimiento_id: Optional[int] = None  # Hacemos que sea opcional con un valor predeterminado

@app.post("/login")
def login(login_request: LoginRequest):
    conexion = conectar_db()
    if conexion is None:
        raise HTTPException(status_code=500, detail="No se pudo conectar a la base de datos")
    
    cursor = None
    try:
        cursor = conexion.cursor(dictionary=True)
        
        # Consulta SQL para obtener el usuario y su rol
        cursor.execute("SELECT * FROM usuarios WHERE usuario = %s AND contraseña = %s", 
                       (login_request.usuario, login_request.contraseña))
        
        # Si hay resultados, el login es exitoso
        usuario = cursor.fetchone()
        if usuario:
            # Depuración: imprimir el usuario encontrado con campos específicos
            print(f"Usuario encontrado - ID: {usuario.get('ID')}, Usuario: {usuario.get('usuario')}, Rol: {usuario.get('Rol')}")
            
            # Devolver respuesta con el ID correcto
            return {
                "success": True, 
                "message": "Login exitoso",
                "rol": usuario.get("Rol"),
                "user_id": usuario.get("ID")  # Ya no modificamos el ID
            }
        else:
            return {"success": False, "message": "Usuario o contraseña incorrectos"}
    
    except Error as e:
        print(f"Error SQL: {e}")
        raise HTTPException(status_code=500, detail=f"Error al consultar la base de datos: {e}")
    
    finally:
        if cursor:
            cursor.close()
        if conexion:
            conexion.close()

@app.get("/procesos")
def obtener_procesos(establecimiento_id: Optional[int] = Query(None)):
    conexion = conectar_db()
    if conexion is None:
        raise HTTPException(status_code=500, detail="No se pudo conectar a la base de datos")
    
    cursor = None
    try:
        cursor = conexion.cursor(dictionary=True)  # Usar cursor de diccionario
        
        # Si tenemos un ID de establecimiento, filtrar por él
        if establecimiento_id is not None:
            cursor.execute("SELECT * FROM procesos2 WHERE establecimiento_id = %s", (establecimiento_id,))
        else:
            # Si no, obtener todos los procesos
            cursor.execute("SELECT * FROM procesos2")
        
        procesos = cursor.fetchall()
        
        # Convertir las fechas a strings si es necesario
        for proceso in procesos:
            if 'fecha_inicio' in proceso and not isinstance(proceso['fecha_inicio'], str):
                proceso['fecha_inicio'] = proceso['fecha_inicio'].strftime('%Y-%m-%d')
            if 'fecha_fin' in proceso and not isinstance(proceso['fecha_fin'], str):
                proceso['fecha_fin'] = proceso['fecha_fin'].strftime('%Y-%m-%d')
        
        return {"procesos": procesos}
    
    except Error as e:
        print(f"Error al consultar procesos: {e}")
        raise HTTPException(status_code=500, detail=f"Error al consultar la base de datos: {e}")
    
    finally:
        if cursor:
            cursor.close()
        if conexion:
            conexion.close()

@app.get("/establecimientos")
def obtener_establecimientos():
    conexion = conectar_db()
    if conexion is None:
        raise HTTPException(status_code=500, detail="No se pudo conectar a la base de datos")
    
    cursor = None
    try:
        cursor = conexion.cursor(dictionary=True)  # Usar cursor de diccionario
        cursor.execute("SELECT * FROM Establecimientos")  # Asegúrate de que la tabla exista en la base de datos
        
        establecimientos = cursor.fetchall()
        
        return {"establecimientos": establecimientos}
    
    except Error as e:
        print(f"Error al consultar establecimientos: {e}")
        raise HTTPException(status_code=500, detail=f"Error al consultar la base de datos: {e}")
    
    finally:
        if cursor:
            cursor.close()
        if conexion:
            conexion.close()

@app.get("/usuario_establecimiento")
def obtener_establecimiento_usuario(usuario_id: int = Query(...)):
    """
    Obtiene el establecimiento asignado a un usuario específico
    a través de la tabla intermedia usuario_establecimiento
    """
    # Añadir log para ver el ID recibido
    print(f"Buscando establecimiento para usuario ID: {usuario_id}")
    
    conexion = conectar_db()
    if conexion is None:
        raise HTTPException(status_code=500, detail="No se pudo conectar a la base de datos")
    
    cursor = None
    try:
        cursor = conexion.cursor(dictionary=True)
        
        # Consulta para obtener el establecimiento asignado al usuario
        query = """
        SELECT e.id, e.nombre, e.direccion, e.tipo, e.estado 
        FROM Establecimientos e 
        JOIN usuario_establecimiento ue ON e.id = ue.establecimiento_id 
        WHERE ue.usuario_id = %s
        """
        
        # Imprimir la consulta para depuración
        print(f"Ejecutando consulta: {query} con usuario_id={usuario_id}")
        
        cursor.execute(query, (usuario_id,))
        establecimiento = cursor.fetchone()
        
        # Verificar el resultado
        print(f"Resultado de la consulta: {establecimiento}")
        
        if establecimiento:
            # Si se encontró un establecimiento, devolver sus datos
            return {
                "success": True,
                "establecimiento_id": establecimiento['id'],
                "establecimiento_nombre": establecimiento['nombre'],
                "establecimiento_direccion": establecimiento.get('direccion', ''),
                "establecimiento_tipo": establecimiento.get('tipo', ''),
                "establecimiento_estado": establecimiento.get('estado', '')
            }
        else:
            # Si no se encontró, devolver un mensaje de error
            return {
                "success": False,
                "message": "No se encontró establecimiento asignado al usuario"
            }
    
    except Error as e:
        print(f"Error al consultar establecimiento del usuario: {e}")
        return {
            "success": False,
            "message": f"Error en la consulta: {str(e)}"
        }
    
    finally:
        if cursor:
            cursor.close()
        if conexion:
            conexion.close()

@app.post("/procesos")
def agregar_proceso(proceso: Proceso):
    conexion = conectar_db()
    if conexion is None:
        raise HTTPException(status_code=500, detail="No se pudo conectar a la base de datos")
    
    cursor = None
    try:
        cursor = conexion.cursor()
        cursor.execute("""
            INSERT INTO procesos2 (
                tipo_proceso, descripcion, frecuencia, horario, 
                fecha_inicio, fecha_fin, estado, establecimiento_id
            ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        """, (
            proceso.tipo_proceso, proceso.descripcion, proceso.frecuencia, 
            proceso.horario, proceso.fecha_inicio, proceso.fecha_fin, 
            proceso.estado, proceso.establecimiento_id
        ))
        
        conexion.commit()
        return {"success": True, "message": "Proceso agregado exitosamente"}
    
    except Error as e:
        raise HTTPException(status_code=500, detail=f"Error al insertar en la base de datos: {e}")
    
    finally:
        if cursor:
            cursor.close()
        if conexion:
            conexion.close()

@app.delete("/procesos/{id}")
def eliminar_proceso(id: int = Path(..., description="ID del proceso a eliminar")):
    conexion = conectar_db()
    if conexion is None:
        raise HTTPException(status_code=500, detail="No se pudo conectar a la base de datos")
    
    cursor = None
    try:
        cursor = conexion.cursor()
        # Consulta SQL para eliminar un proceso por ID
        cursor.execute("DELETE FROM procesos2 WHERE id = %s", (id,))
        
        conexion.commit()
        
        # Verificar si se eliminó algún registro
        if cursor.rowcount > 0:
            return {"success": True, "message": "Proceso eliminado exitosamente"}
        else:
            raise HTTPException(status_code=404, detail="No se encontró el proceso especificado")
    
    except Error as e:
        raise HTTPException(status_code=500, detail=f"Error al eliminar de la base de datos: {e}")
    
    finally:
        if cursor:
            cursor.close()
        if conexion:
            conexion.close()

@app.post("/asignar_establecimiento")
def asignar_establecimiento(usuario_id: int = Query(...), establecimiento_id: int = Query(...)):
    """
    Asigna un establecimiento a un usuario a través de la tabla intermedia
    """
    conexion = conectar_db()
    if conexion is None:
        raise HTTPException(status_code=500, detail="No se pudo conectar a la base de datos")
    
    cursor = None
    try:
        cursor = conexion.cursor()
        
        # Verificar si la asignación ya existe
        cursor.execute(
            "SELECT * FROM usuario_establecimiento WHERE usuario_id = %s AND establecimiento_id = %s", 
            (usuario_id, establecimiento_id)
        )
        
        if cursor.fetchone():
            return {
                "success": False,
                "message": "Este usuario ya tiene asignado este establecimiento"
            }
        
        # Insertar la nueva asignación
        cursor.execute(
            "INSERT INTO usuario_establecimiento (usuario_id, establecimiento_id) VALUES (%s, %s)",
            (usuario_id, establecimiento_id)
        )
        
        conexion.commit()
        
        return {
            "success": True,
            "message": "Establecimiento asignado correctamente al usuario"
        }
    
    except Error as e:
        print(f"Error al asignar establecimiento: {e}")
        return {
            "success": False,
            "message": f"Error al asignar: {str(e)}"
        }
    
    finally:
        if cursor:
            cursor.close()
        if conexion:
            conexion.close()