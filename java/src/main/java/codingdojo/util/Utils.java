package codingdojo.util;

import codingdojo.misc.Customer;
import codingdojo.misc.CustomerDataLayer;
import codingdojo.match.CustomerMatches;
import codingdojo.misc.ShoppingList;

public class Utils {
    public static void subLoadCompanyCustomer(CustomerMatches matches, Customer matchByExternalId, Customer matchByMasterId, Customer matchByCompanyNumber) {
        if (matchByExternalId != null) {
            matches.setCustomer(matchByExternalId);
            matches.setMatchTerm("ExternalId");
            if (matchByMasterId != null) matches.addDuplicate(matchByMasterId);
        } else {
            if (matchByCompanyNumber != null) {
                matches.setCustomer(matchByCompanyNumber);
                matches.setMatchTerm("CompanyNumber");
            }
        }
    }

    public static void updateShoppingList(Customer customer, ShoppingList consumerShoppingList, CustomerDataLayer customerDataLayer1) {
        customer.addShoppingList(consumerShoppingList);
        customerDataLayer1.updateShoppingList(consumerShoppingList);
        customerDataLayer1.upd(customer);
    }
}
