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

// Persistencia del bloqueo efímero de butacas; único SQL nativo del backend.
// Tomar = UPDATE condicional (renovar) y, si no había fila, INSERT IGNORE: la atomicidad la da la base.
// REQUIRES_NEW acorta los locks: el UPDATE sin fila deja un gap lock que, con dos sesiones, termina en deadlock.
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
