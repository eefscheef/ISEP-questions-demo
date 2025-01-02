import org.hibernate.Session
import org.hibernate.SessionFactory


class DatabaseManager(private val sessionFactory: SessionFactory) {

    fun getSession(): Session {
        return sessionFactory.openSession()
    }

    fun clearDatabase() {
        getSession().use { session ->
            try {
                session.beginTransaction()

                // Raw SQL queries for truncating tables
                session.createNativeMutationQuery("TRUNCATE TABLE assessment CASCADE").executeUpdate()
                session.createNativeMutationQuery("TRUNCATE TABLE section CASCADE").executeUpdate()
                session.createNativeMutationQuery("TRUNCATE TABLE assignment CASCADE").executeUpdate()

                // Commit the transaction
                session.transaction.commit()
            } catch (e: Exception) {
                // Rollback in case of an error
                session.transaction.rollback()
                throw e
            }
        }
    }


    fun uploadEntities(entities: List<Any>) {
        sessionFactory.openSession().use { session ->
            val transaction = session.beginTransaction()
            try {
                entities.forEach { session.save(it) }
                transaction.commit()
            } catch (exception: Exception) {
                transaction.rollback()
                throw exception
            }
        }
    }

}
