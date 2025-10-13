package com.smooy.smooypr1.API;

import com.smooy.smooypr1.Establecimientos;
import com.smooy.smooypr1.avisos.Aviso;
import com.smooy.smooypr1.avisos.AvisoImagen;
import com.smooy.smooypr1.avisos.AvisoComentario; // Agregar esta importación
import com.smooy.smooypr1.avisos.AvisoMasivo;
import com.smooy.smooypr1.procesos.EstadoResponse;
import com.smooy.smooypr1.procesos.Proceso;
import com.smooy.smooypr1.procesos.ProcesosResponse;
import com.smooy.smooypr1.procesos.ProcesoComentario;
import com.smooy.smooypr1.procesos.ProcesoImagen;
import com.smooy.smooypr1.procesos.ComentariosResponse;
import com.smooy.smooypr1.usuarios.Usuario;
import com.smooy.smooypr1.tareas.Tarea;
import com.smooy.smooypr1.tareas.TareaComentario;
import com.smooy.smooypr1.tareas.TareaImagen;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @GET("procesos")
    Call<ProcesosResponse> obtenerProcesos();

    // Agregar estos métodos a tu interfaz ApiService existente:

    // Método para envío masivo de avisos (para admins)
    @POST("avisos/masivo")
    Call<Map<String, Object>> enviarAvisoMasivo(@Body AvisoMasivo avisoMasivo);

    // Método para obtener todos los establecimientos (para admins)
    @GET("establecimientos/todos")
    Call<Map<String, List<Establecimientos>>> obtenerTodosLosEstablecimientos();

    @GET("procesos")
    Call<ProcesosResponse> obtenerProcesos(@Query("establecimiento_id") int establecimientoId);

    @POST("procesos")
    Call<Void> agregarProceso(@Body Proceso proceso);

    @POST("procesos-adaptado")
    Call<Map<String, Object>> agregarProceso(@Body Map<String, Object> procesoMap);

    @DELETE("procesos/{id}")
    Call<Void> eliminarProceso(@Path("id") int id);

    // Corregir el método para usar la misma clase Establecimientos en toda la aplicación
    @GET("establecimientos")
    Call<Map<String, List<com.smooy.smooypr1.Establecimientos>>> obtenerEstablecimientos();

    // Añadir el método faltante para obtener establecimientos de un usuario
    @GET("usuario_establecimiento")
    Call<Map<String, List<Establecimientos>>> obtenerEstablecimientosUsuario(@Query("usuario_id") int usuarioId);

    // Obtener proceso por ID
    @GET("procesos/{id}")
    Call<Proceso> obtenerProcesoPorId(@Path("id") int procesoId);

    // Get proceso related to a task
    @GET("tareas/{id}/proceso")
    Call<Map<String, Object>> obtenerProcesoTarea(@Path("id") int tareaId);

    // Process comments
    @GET("procesos/{id}/comentarios")
    Call<ComentariosResponse> obtenerComentariosProceso(@Path("id") int procesoId);

    // Agregar comentario a un proceso (ÚNICO MÉTODO, ELIMINAR EL DUPLICADO)
    @POST("procesos/{id}/comentarios")
    Call<ProcesoComentario> agregarComentario(@Path("id") int procesoId, @Body ProcesoComentario comentario);

    // Process images
    @GET("procesos/{id}/imagenes")
    Call<List<ProcesoImagen>> obtenerImagenesProceso(@Path("id") int procesoId);

    @GET("imagenes/{imagenId}")
    Call<ResponseBody> descargarImagen(@Path("imagenId") int imagenId);

    // Subir imagen a un proceso (ÚNICO MÉTODO, ELIMINAR EL DUPLICADO)
    @Multipart
    @POST("procesos/{proceso_id}/imagenes")
    Call<ProcesoImagen> subirImagen(
            @Path("proceso_id") int procesoId,
            @Part("usuario_id") RequestBody usuarioId,
            @Part MultipartBody.Part imagen
    );

    @POST("avisos/")  // Elimina espacios innecesarios
    Call<Aviso> agregarAviso(@Body Aviso aviso);

    @DELETE("avisos/{id}")
    Call<Void> borrarAviso(@Path("id") int id);

    @GET("avisos")  // Cambiar de "/avisos/" a "avisos"
    Call<Map<String, List<Aviso>>> obtenerAvisos(@Query("establecimiento_id") Integer establecimientoId);

    @PUT("avisos/{id}")
    Call<Aviso> actualizarAviso(@Path("id") int id, @Body Aviso aviso);

    // Añadir este nuevo método para actualizar solo el estado
    @PUT("avisos/{id}/estado")
    Call<Aviso> actualizarAvisoEstado(@Path("id") int id, @Body Map<String, String> estado);

    // Añadir estos nuevos endpoints para manejar imágenes de avisos
    @GET("avisos/{id}/imagenes")
    Call<List<AvisoImagen>> obtenerImagenesAviso(@Path("id") int avisoId);

    @Multipart
    @POST("avisos/{aviso_id}/imagenes")
    Call<AvisoImagen> subirImagenAviso(
            @Path("aviso_id") int avisoId,
            @Part("usuario_id") RequestBody usuarioId,
            @Part MultipartBody.Part imagen
    );

    @GET("establecimientos/usuario/{usuarioId}")
    Call<Map<String, List<Establecimientos>>> obtenerEstablecimientosPorUsuario(@Path("usuarioId") int usuarioId);

    @GET("avisos/{id}")
    Call<Aviso> obtenerAvisoPorId(@Path("id") int avisoId);


    @GET("avisos/{avisoId}/comentarios")
    Call<List<AvisoComentario>> obtenerComentariosAviso(@Path("avisoId") int avisoId);

    @POST("avisos/{avisoId}/comentarios")
    Call<AvisoComentario> agregarComentarioAviso(@Path("avisoId") int avisoId, @Body Map<String, Object> comentarioData);

    @GET("debug/headers")
    Call<Map<String, Object>> obtenerHeaders();

    @GET("verify-token")
    Call<Map<String, Object>> verifyToken();

    // Añadir este nuevo método a la interfaz
    @POST("registro")
    Call<Map<String, Object>> crearUsuario(@Body Map<String, Object> usuario);

    // Añadir estos métodos a la interfaz existente:
    @GET("usuarios")
    Call<Map<String, List<Usuario>>> obtenerUsuarios();

    @DELETE("usuarios/{id}")
    Call<Map<String, Object>> eliminarUsuario(@Path("id") int id);

    // Add this method to the ApiService interface
    @PUT("usuarios/{id}")
    Call<Map<String, Object>> actualizarUsuario(@Path("id") int id, @Body Map<String, Object> usuario);

    // Endpoints para gestión de tareas
    @GET("procesos/{procesoId}/tareas")
    Call<List<Tarea>> obtenerTareasProceso(@Path("procesoId") int procesoId);

    @POST("procesos/{procesoId}/tareas")
    Call<Map<String, Object>> agregarTareaProceso(
            @Path("procesoId") int procesoId,
            @Body Map<String, Object> tareaDatos);

    @DELETE("tareas/{tareaId}")
    Call<Void> eliminarTarea(@Path("tareaId") int tareaId);

    @POST("procesos/{procesoId}/generar-tareas")
    Call<Map<String, Object>> generarTareasProceso(
            @Path("procesoId") int procesoId);

    @POST("tareas")
    Call<Map<String, Object>> crearTarea(@Body Map<String, Object> tareaDatos);

    @DELETE("establecimientos/{id}")
    Call<Map<String, Object>> eliminarEstablecimiento(@Path("id") int id);

    @POST("cambiar-password")
    Call<Map<String, Object>> cambiarPassword(@Body Map<String, Object> datos);

    @POST("establecimientos")
    Call<Map<String, Object>> crearEstablecimiento(@Body Map<String, Object> establecimiento);

    // Añadir estos métodos a la interfaz existente:

    @GET("tareas/{id}")
    Call<Tarea> obtenerTareaPorId(@Path("id") int tareaId);

    @GET("tareas/{tareaId}/comentarios")
    Call<Map<String, List<TareaComentario>>> obtenerComentariosTarea(@Path("tareaId") int tareaId);

    @POST("tareas/{tareaId}/comentarios")
    Call<TareaComentario> agregarComentarioTarea(@Path("tareaId") int tareaId, @Body TareaComentario comentario);

    @GET("tareas/{tareaId}/imagenes")
    Call<Map<String, List<TareaImagen>>> obtenerImagenesTarea(@Path("tareaId") int tareaId);

    @Multipart
    @POST("tareas/{tarea_id}/imagenes")
    Call<TareaImagen> subirImagenTarea(
            @Path("tarea_id") int tareaId,
            @Part("usuario_id") RequestBody usuarioId,
            @Part MultipartBody.Part imagen
    );

    @PUT("tareas/{id}")
    Call<Map<String, Object>> actualizarTarea(@Path("id") int tareaId, @Body Map<String, Object> datos);

    // Update task status
    @PUT("tareas/{id}")
    Call<Tarea> actualizarEstadoTarea(@Path("id") int tareaId, @Body Map<String, Object> data);

    // Creates tables for tareas
    @POST("tareas/crear-tablas")
    Call<Map<String, Object>> crearTablasTareas();

    @DELETE("tareas/{tarea_id}/imagenes/{imagen_id}")
    Call<Map<String, Object>> eliminarImagenTarea(@Path("tarea_id") int tareaId, @Path("imagen_id") int imagenId);

    @DELETE("tareas/{tarea_id}/comentarios/{comentario_id}")
    Call<Map<String, Object>> eliminarComentarioTarea(@Path("tarea_id") int tareaId, @Path("comentario_id") int comentarioId);

    @DELETE("avisos/{aviso_id}/imagenes/{imagen_id}")
    Call<Map<String, Object>> eliminarImagenAviso(@Path("aviso_id") int avisoId, @Path("imagen_id") int imagenId);

    @DELETE("avisos/{aviso_id}/comentarios/{comentario_id}")
    Call<Map<String, Object>> eliminarComentarioAviso(@Path("aviso_id") int avisoId, @Path("comentario_id") int comentarioId);

    @DELETE("/procesos/{proceso_id}/comentarios/{comentario_id}")
    Call<Map<String, Object>> eliminarComentarioProceso(@Path("proceso_id") int procesoId, @Path("comentario_id") int comentarioId);

    @PUT("procesos/{proceso_id}/verificar-completado")
    Call<Map<String, Object>> verificarCompletado(@Path("proceso_id") int procesoId);

    @PUT("procesos/{proceso_id}/estado")
    Call<EstadoResponse> actualizarEstadoProceso(@Path("proceso_id") int procesoId, @Body Map<String, String> estado);
}





