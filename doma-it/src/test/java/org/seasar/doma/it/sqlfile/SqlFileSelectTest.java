package org.seasar.doma.it.sqlfile;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.runner.RunWith;
import org.seasar.doma.it.dao.EmployeeDao;
import org.seasar.doma.it.dao.EmployeeDao_;
import org.seasar.doma.it.entity.Employee;
import org.seasar.framework.unit.Seasar2;

@RunWith(Seasar2.class)
public class SqlFileSelectTest {

    public void testEmbeddedVariable() throws Exception {
        EmployeeDao dao = new EmployeeDao_();
        List<Employee> list = dao.selectWithOptionalOrderBy("S",
                "order by EMPLOYEE_ID");
        assertEquals(2, list.size());
        assertEquals(new Integer(1), list.get(0).getEmployee_id());
        assertEquals(new Integer(8), list.get(1).getEmployee_id());

        list = dao.selectWithOptionalOrderBy("S", "order by EMPLOYEE_ID desc");
        assertEquals(2, list.size());
        assertEquals(new Integer(8), list.get(0).getEmployee_id());
        assertEquals(new Integer(1), list.get(1).getEmployee_id());
    }

    public void testNesteadIf() throws Exception {
        Employee example = new Employee();
        example.setEmployee_no(7801);
        example.setManager_id(1);
        example.setSalary(new BigDecimal("5000"));
        EmployeeDao dao = new EmployeeDao_();
        List<Employee> list = dao.selectByExample(example);
        assertEquals(1, list.size());

        example.setEmployee_no(7777);
        list = dao.selectByExample(example);
        assertEquals(14, list.size());
    }
}