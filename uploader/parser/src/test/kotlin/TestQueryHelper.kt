package ut.isep

import org.hibernate.Session
import ut.isep.management.model.entity.BaseEntity
import java.util.function.Function

object TestQueryHelper {
    inline fun <reified T> fetchAll(session: Session): List<T> {
        val cb = session.criteriaBuilder
        val query = cb.createQuery(T::class.java)
        query.select(query.from(T::class.java))
        return session.createQuery(query).resultList
    }

    inline fun <reified T> fetchSingle(session: Session): T? {
        val cb = session.criteriaBuilder
        val query = cb.createQuery(T::class.java)
        query.select(query.from(T::class.java))
        return session.createQuery(query).singleResultOrNull
    }

    fun <T : BaseEntity<*>> persistEntity(entity: T, session: Session): T {
        val transaction = session.beginTransaction()
        return try {
            session.persist(entity)
            transaction.commit()
            entity // Return the persisted entity
        } catch (exception: Exception) {
            transaction.rollback()
            throw exception
        }
    }
}