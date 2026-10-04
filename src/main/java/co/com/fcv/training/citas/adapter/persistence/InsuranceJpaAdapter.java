package co.com.fcv.training.citas.adapter.persistence;

import co.com.fcv.training.citas.application.Ports;
import co.com.fcv.training.citas.application.SchedulingFailure;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
class InsuranceJpaAdapter implements Ports.Affiliations {
    private final JdbcTemplate jdbc;
    InsuranceJpaAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void createCurrent(Long userId, Long planId) {
        requireSelectablePlan(planId);
        jdbc.update("insert into user_insurance_affiliations(user_id,plan_id,membership_number,is_current) values (?,?,?,true)", userId, planId, membership(userId, planId));
    }

    public Optional<Ports.Affiliation> current(Long userId) {
        return jdbc.query("select p.id,p.code,p.name,e.id,e.name,r.id,r.name,a.membership_number from user_insurance_affiliations a join eps_plans p on p.id=a.plan_id join eps e on e.id=p.eps_id join insurance_regimes r on r.id=p.regime_id where a.user_id=? and a.is_current=true order by a.id desc limit 1",
                (r, n) -> new Ports.Affiliation(r.getLong(1), r.getString(2), r.getString(3), r.getLong(4), r.getString(5), r.getLong(6), r.getString(7), r.getString(8)), userId).stream().findFirst();
    }

    @Transactional
    public Ports.Affiliation changeCurrent(Long userId, Long planId) {
        requireSelectablePlan(planId);
        jdbc.queryForList("select id from user_insurance_affiliations where user_id=? for update", Long.class, userId);
        Long currentPlan = jdbc.queryForList("select plan_id from user_insurance_affiliations where user_id=? and is_current=true", Long.class, userId).stream().findFirst().orElse(null);
        if (planId.equals(currentPlan)) throw new SchedulingFailure(SchedulingFailure.Kind.CONFLICT, "El plan ya es tu afiliación vigente");
        jdbc.update("update user_insurance_affiliations set is_current=false where user_id=? and is_current=true", userId);
        // Returning to a previously used plan reactivates its row instead of duplicating user/plan.
        Long previous = jdbc.queryForList("select id from user_insurance_affiliations where user_id=? and plan_id=? order by id limit 1", Long.class, userId, planId).stream().findFirst().orElse(null);
        if (previous != null) jdbc.update("update user_insurance_affiliations set is_current=true where id=?", previous);
        else jdbc.update("insert into user_insurance_affiliations(user_id,plan_id,membership_number,is_current) values (?,?,?,true)", userId, planId, membership(userId, planId));
        return current(userId).orElseThrow();
    }

    /** Only an active plan of an active EPS can be selected. */
    private void requireSelectablePlan(Long planId) {
        Integer count = jdbc.queryForObject("select count(*) from eps_plans p join eps e on e.id=p.eps_id where p.id=? and p.active=true and e.active=true", Integer.class, planId);
        if (count == null || count == 0) throw new IllegalArgumentException("Plan de afiliación inválido");
    }

    private String membership(Long userId, Long planId) { return "AUTO-" + userId + "-" + planId; }
}
