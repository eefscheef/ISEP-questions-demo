package ut.isep

import org.hibernate.Session
import ut.isep.management.model.entity.BaseEntity

object TestQueryHelper {
    inline fun <reified T> fetchAll(session: Session): List<T> {
        val cb = session.criteriaBuilder
        val query = cb.createQuery(T::class.java)
        query.select(query.from(T::class.java))
        return session.createQuery(query).resultList
    }

    inline fun <reified T> fetchAll(
        session: Session,
        noinline filter: ((T) -> Boolean)? = null
    ): List<T> {
        val cb = session.criteriaBuilder
        val query = cb.createQuery(T::class.java)
        val root = query.from(T::class.java)
        query.select(root)

        // If a filter is provided, apply it
        val result = session.createQuery(query).resultList
        return if (filter != null) {
            result.filter { filter(it) } // Apply filter condition
        } else {
            result
        }
    }

    inline fun <reified T : BaseEntity<*>> fetchSingle(
        session: Session,
        noinline filter: ((T) -> Boolean)? = null
    ): T? {
        val cb = session.criteriaBuilder
        val query = cb.createQuery(T::class.java)
        val root = query.from(T::class.java)
        query.select(root)

        // If a filter is provided, apply it
        val result = session.createQuery(query).resultList
        return if (filter != null) {
            result.find { filter(it) } // Apply filter condition
        } else {
            result.singleOrNull()
        }
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