package codingdojo.misc;

import codingdojo.match.CustomerMatches;
import codingdojo.util.Utils;

public class CustomerDataAccess {

    public final CustomerDataLayer cdl;

    public CustomerDataAccess(CustomerDataLayer cdl) {
        this.cdl = cdl;
    }

    public CustomerMatches loadCompanyCustomer(String eid, String cn) {
        CustomerMatches m = new CustomerMatches();
        Customer mbei = this.cdl.find2(eid);
        Utils.subLoadCompanyCustomer(m, mbei, this.cdl.findByMasterExternalId(eid), this.cdl.find(cn));

        return m;
    }

    public CustomerMatches loadPersonCustomer(String eid) {
        CustomerMatches m = new CustomerMatches();
        Customer mbpn = this.cdl.find2(eid);
        m.setCustomer(mbpn);
        if (mbpn != null) m.setMatchTerm("ExternalId");
        return m;
    }

    public Customer upd(Customer customer) {
        return cdl.upd(customer);
    }

    public Customer crt(Customer customer) {
        return cdl.crt(customer);
    }

}
