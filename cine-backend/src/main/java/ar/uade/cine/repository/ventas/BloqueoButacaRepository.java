package ar.uade.cine.repository.ventas;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.uade.cine.model.ventas.BloqueoButaca;

// Persistencia del bloqueo efímero de butacas; Repository con el único SQL nativo del backend.
// Tomar una butaca son dos sentencias y ninguna lee antes de escribir: renovar y, si no
// había fila que renovar, insertar. La atomicidad la da la base —el UPDATE condicional
// bloquea la fila y reevalúa el WHERE, y la clave primaria no deja entrar un segundo
// INSERT—, así que no hace falta @Lock(PESSIMISTIC_WRITE) ni un SELECT ... FOR UPDATE.
//
// INSERT IGNORE y no un INSERT que choque: perder la butaca es el caso normal, no un
// error. Con el INSERT común cada pérdida era una excepción que deja la transacción
// rollback-only y un ERROR de Hibernate en el log; así es un 0 en filas afectadas. H2 lo
// entiende en MODE=MySQL, que es lo que corre la suite.
//
// Las dos van en su propia transacción (REQUIRES_NEW) para que sus locks duren lo mínimo
// y no queden atados a la transacción de quien llama. En MySQL importa más de lo que
// parece: el UPDATE que no encuentra fila deja un gap lock hasta el commit, y dos
// sesiones con el mismo gap lock que después insertan se matan en un deadlock. Cerrando
// el UPDATE antes del INSERT, el segundo INSERT espera al primero y lo ignora.
public interface BloqueoButacaRepository extends JpaRepository<BloqueoButaca, BloqueoButaca.Clave> {

    @Query("""
            select b from BloqueoButaca b
            where b.funcion.id = :funcion and b.venceEn > :ahora""")
    List<BloqueoButaca> vigentes(@Param("funcion") int funcionId, @Param("ahora") LocalDateTime ahora);

    // Renueva la propia o se queda con una vencida de otro. 0 filas: no había, o es de otro.
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("""
            update BloqueoButaca b set b.sesion = :sesion, b.venceEn = :vence
            where b.funcion.id = :funcion and b.asiento.id = :asiento
              and (b.sesion = :sesion or b.venceEn <= :ahora)""")
    int renovarOTomarVencida(@Param("funcion") int funcionId, @Param("asiento") int asientoId,
                             @Param("sesion") String sesion, @Param("vence") LocalDateTime vence,
                             @Param("ahora") LocalDateTime ahora);

    // 0 filas: otra sesión la insertó primero.
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(nativeQuery = true, value = """
            INSERT IGNORE INTO bloqueo_butaca (funcion_id, asiento_id, sesion, vence_en)
            VALUES (:funcion, :asiento, :sesion, :vence)""")
    int insertarSiNoEsta(@Param("funcion") int funcionId, @Param("asiento") int asientoId,
                         @Param("sesion") String sesion, @Param("vence") LocalDateTime vence);

    // Filtra por dueño: una sesión no puede soltar la butaca de otra.
    @Modifying
    @Transactional
    @Query("delete from BloqueoButaca b where b.funcion.id = :funcion and b.sesion = :sesion")
    int liberar(@Param("funcion") int funcionId, @Param("sesion") String sesion);

    @Modifying
    @Transactional
    @Query("""
            delete from BloqueoButaca b
            where b.funcion.id = :funcion and b.sesion = :sesion and b.asiento.id not in :conservar""")
    int liberarMenos(@Param("funcion") int funcionId, @Param("sesion") String sesion,
                     @Param("conservar") Collection<Integer> conservar);

    @Modifying
    @Transactional
    @Query("delete from BloqueoButaca b where b.venceEn <= :ahora")
    int borrarVencidos(@Param("ahora") LocalDateTime ahora);
}
