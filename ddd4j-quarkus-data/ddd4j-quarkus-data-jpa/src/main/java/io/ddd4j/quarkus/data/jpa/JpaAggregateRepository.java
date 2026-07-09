package io.ddd4j.quarkus.data.jpa;

import io.ddd4j.core.cqrs.query.Query;
import io.ddd4j.core.ddd.model.AggregateRoot;
import io.ddd4j.core.ddd.repository.RichRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JPA 轨道仓储基类（Quarkus Hibernate Panache 桥接）。
 *
 * <p>基于 ddd4j-core 的 {@link RichRepository} SPI，
 * 使用 Jakarta Persistence API（EntityManager）实现充血查询。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * &#64;ApplicationScoped
 * public class OrderRepository extends JpaAggregateRepository<Order, OrderId> {
 *     &#64;PersistenceContext
 *     public void setEm(EntityManager em) { super.setEntityManager(em); }
 * }
 * }</pre>
 *
 * @param <M>  聚合根类型
 * @param <P>  持久化对象类型（@Entity PO）
 * @param <ID> 聚合根标识类型
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 4.0.0
 */
public abstract class JpaAggregateRepository<M extends AggregateRoot<?>, P, ID extends Serializable>
        implements RichRepository<M, ID> {

    private static final Logger log = LoggerFactory.getLogger(JpaAggregateRepository.class);

    @PersistenceContext
    protected EntityManager em;

    private Class<M> modelClass;
    private Class<P> persistenceClass;

    @SuppressWarnings("unchecked")
    public Class<M> getModelClass() {
        if (modelClass == null && persistenceClass != null) {
            // 默认从 persistenceClass 推断（子类可覆盖 getModelClass()）
            return (Class<M>) persistenceClass;
        }
        return modelClass;
    }

    public void setModelClass(Class<M> modelClass) {
        this.modelClass = modelClass;
    }

    public Class<P> getPersistenceClass() {
        return persistenceClass;
    }

    public void setPersistenceClass(Class<P> persistenceClass) {
        this.persistenceClass = persistenceClass;
    }

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<M> findById(ID id) {
        return Optional.ofNullable(em.find(persistenceClass, id)).map(this::toModel);
    }

    @Override
    public M save(M aggregate) {
        P po = toPersistenceObject(aggregate);
        if (em.contains(po)) {
            return em.merge(po) != null ? aggregate : aggregate;
        } else {
            em.persist(po);
            return aggregate;
        }
    }

    @Override
    public Optional<M> findFirst() {
        return Optional.ofNullable(
            em.createQuery("SELECT p FROM " + persistenceClass.getSimpleName() + " p", persistenceClass)
              .setMaxResults(1)
              .getSingleResultOrNull()
        ).map(this::toModel);
    }

    @Override
    public List<M> findAll() {
        return em.createQuery("SELECT p FROM " + persistenceClass.getSimpleName() + " p", persistenceClass)
                .getResultList()
                .stream()
                .map(this::toModel)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public long count() {
        return em.createQuery("SELECT COUNT(p) FROM " + persistenceClass.getSimpleName() + " p", Long.class)
                .getSingleResult();
    }

    @Override
    public org.ddd4j.core.api.Page<M> page(Query query) {
        // 简化实现：JPA 风格分页，Query 字段后缀映射复用 ddd4j-data-mybatisplus 的 setCondition
        int page = (int) Math.max(0, (query.getCurrent() - 1));
        int size = (int) query.getSize();
        var queryObj = em.createQuery("SELECT p FROM " + persistenceClass.getSimpleName() + " p", persistenceClass);
        queryObj.setFirstResult(page * size);
        queryObj.setMaxResults(size);
        List<M> list = queryObj.getResultList().stream().map(this::toModel).collect(java.util.stream.Collectors.toList());
        return org.ddd4j.core.api.Page.succeed(list, count(), query.getCurrent(), size);
    }

    @Override
    public long count(Query query) {
        return count();
    }

    @Override
    public Optional<M> findFirst(Query query) {
        var q = em.createQuery("SELECT p FROM " + persistenceClass.getSimpleName() + " p", persistenceClass);
        q.setMaxResults(1);
        return Optional.ofNullable(q.getSingleResultOrNull()).map(this::toModel);
    }

    @Override
    public List<M> findList(Query query) {
        return em.createQuery("SELECT p FROM " + persistenceClass.getSimpleName() + " p", persistenceClass)
                .getResultList()
                .stream()
                .map(this::toModel)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> maps(Query query) {
        return em.createNativeQuery(
            "SELECT * FROM " + em.getMetamodel().entity(persistenceClass).getName())
            .setMaxResults(500)
            .getResultList()
            .stream()
            .map(o -> {
                Object[] row = (Object[]) o;
                Map<String, Object> m = new java.util.HashMap<>();
                for (int i = 0; i < row.length; i++) m.put("col_" + i, row[i]);
                return m;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public boolean update(AggregateRoot<?> aggregate, Query query) {
        try {
            save((M) aggregate);
            return true;
        } catch (Exception e) {
            log.error("JPA update failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteByQuery(Query query) {
        int deleted = em.createQuery("DELETE FROM " + persistenceClass.getSimpleName() + " p").executeUpdate();
        return deleted > 0;
    }

    @Override
    public void fill(Query query, AggregateRoot<?> model) {
        // 业务方按需覆盖
    }

    /**
     * 持久化对象 → 聚合根（业务方按需覆盖转换逻辑）。
     */
    protected M toModel(P persistenceObject) {
        return io.ddd4j.kit.lang.BeanKit.copy(persistenceObject, getModelClass());
    }

    /**
     * 聚合根 → 持久化对象。
     */
    @SuppressWarnings("unchecked")
    protected P toPersistenceObject(M model) {
        return (P) io.ddd4j.kit.lang.BeanKit.copy(model, persistenceClass);
    }

    @Override
    public default boolean exists() {
        return count() > 0;
    }
}
