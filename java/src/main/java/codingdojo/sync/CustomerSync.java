package codingdojo.sync;

import codingdojo.misc.Customer;
import codingdojo.misc.CustomerDataAccess;
import codingdojo.misc.CustomerDataLayer;
import codingdojo.misc.ShoppingList;
import codingdojo.external.ExternalCustomer;
import codingdojo.match.CustomerMatches;
import codingdojo.match.CustomerType;
import codingdojo.service.ConflictException;
import codingdojo.util.Utils;

import java.util.List;

public class CustomerSync {

    private final CustomerDataAccess customerDataAccess;

    public CustomerSync(CustomerDataLayer customerDataLayer) {
        this(new CustomerDataAccess(customerDataLayer));
    }

    public CustomerSync(CustomerDataAccess db) {
        this.customerDataAccess = db;
    }

    public boolean syncWithDataLayer(ExternalCustomer externalCustomer) {

        CustomerMatches customerMatches;
        if (externalCustomer.isCompany()) {
            customerMatches = loadCompany(externalCustomer);
        } else {
            customerMatches = loadPerson(externalCustomer);
        }
        Customer customer = customerMatches.getCustomer();

        if (customer == null) {
            customer = new Customer();
            customer.setExternalId(externalCustomer.getExternalId());
            customer.setMasterExternalId(externalCustomer.getExternalId());
        }

        populateFields(externalCustomer, customer);

        boolean created = false;
        if (customer.getInternalId() == null) {
            customer = createCustomer(customer);
            created = true;
        } else {
            updateCustomer(customer);
        }
        updateContactInfo(externalCustomer, customer);

        if (customerMatches.hasDuplicates()) {
            for (Customer duplicate : customerMatches.getDuplicates()) {
                updateDuplicate(externalCustomer, duplicate);
            }
        }

        updateRelations(externalCustomer, customer);
        updatePreferredStore(externalCustomer, customer);

        return created;
    }

    private void updateRelations(ExternalCustomer externalCustomer, Customer customer) {
        List<ShoppingList> consumerShoppingLists = externalCustomer.getShoppingLists();
        for (ShoppingList consumerShoppingList : consumerShoppingLists) {
            Utils.updateShoppingList(customer, consumerShoppingList, this.customerDataAccess.cdl);
        }
    }

    private Customer updateCustomer(Customer customer) {
        return this.customerDataAccess.upd(customer);
    }

    private void updateDuplicate(ExternalCustomer externalCustomer, Customer duplicate) {
        if (duplicate == null) {
            duplicate = new Customer();
            duplicate.setExternalId(externalCustomer.getExternalId());
            duplicate.setMasterExternalId(externalCustomer.getExternalId());
        }

        duplicate.setName(externalCustomer.getName());

        if (duplicate.getInternalId() == null) {
            createCustomer(duplicate);
        } else {
            updateCustomer(duplicate);
        }
    }

    private void updatePreferredStore(ExternalCustomer externalCustomer, Customer customer) {
        customer.setPreferredStore(externalCustomer.getPreferredStore());
    }

    private Customer createCustomer(Customer customer) {
        return this.customerDataAccess.crt(customer);
    }

    private void populateFields(ExternalCustomer externalCustomer, Customer customer) {
        customer.setName(externalCustomer.getName());
        if (externalCustomer.isCompany()) {
            customer.setCompanyNumber(externalCustomer.getCompanyNumber());
            customer.setCustomerType(CustomerType.COMPANY);
        } else {
            customer.setCustomerType(CustomerType.PERSON);
        }
    }

    private void updateContactInfo(ExternalCustomer externalCustomer, Customer customer) {
        customer.setAddress(externalCustomer.getPostalAddress());
    }

    public CustomerMatches loadCompany(ExternalCustomer externalCustomer) {

        final String externalId = externalCustomer.getExternalId();
        final String companyNumber = externalCustomer.getCompanyNumber();

        CustomerMatches customerMatches = customerDataAccess.loadCompanyCustomer(externalId, companyNumber);

        if (customerMatches.getCustomer() != null && !CustomerType.COMPANY.equals(customerMatches.getCustomer().getCustomerType())) {
            throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a company");
        }

        if (customerMatches.getMatchTerm().equals("ExternalId")) {
            String customerCompanyNumber = customerMatches.getCustomer().getCompanyNumber();
            if (!companyNumber.equals(customerCompanyNumber)) {
                customerMatches.getCustomer().setMasterExternalId(null);
                customerMatches.addDuplicate(customerMatches.getCustomer());
                customerMatches.setCustomer(null);
                customerMatches.setMatchTerm(null);
            }
        } else if (customerMatches.getMatchTerm().equals("CompanyNumber")) {
            String customerExternalId = customerMatches.getCustomer().getExternalId();
            if (customerExternalId != null && !externalId.equals(customerExternalId)) {
                throw new ConflictException("Existing customer for externalCustomer " + companyNumber + " doesn't match external id " + externalId + " instead found " + customerExternalId );
            }
            Customer customer = customerMatches.getCustomer();
            customer.setExternalId(externalId);
            customer.setMasterExternalId(externalId);
            customerMatches.addDuplicate(null);
        }

        return customerMatches;
    }

    public CustomerMatches loadPerson(ExternalCustomer externalCustomer) {
        final String externalId = externalCustomer.getExternalId();

        CustomerMatches customerMatches = customerDataAccess.loadPersonCustomer(externalId);

        if (customerMatches.getCustomer() != null) {
            if (!customerMatches.getCustomer().getCustomerType().equals(CustomerType.PERSON)) {
                throw new ConflictException("Existing customer for externalCustomer " + externalId + " already exists and is not a person");
            }

            if ("ExternalId" != customerMatches.getMatchTerm()) {
                Customer customer = customerMatches.getCustomer();
                customer.setExternalId(externalId);
                customer.setMasterExternalId(externalId);
            }
        }

        return customerMatches;
    }
}