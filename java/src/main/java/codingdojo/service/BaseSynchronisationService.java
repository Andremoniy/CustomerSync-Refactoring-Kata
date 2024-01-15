package codingdojo.service;

import codingdojo.model.*;
import codingdojo.repository.ShoppingListRepository;

import java.util.List;

public class BaseSynchronisationService<ExternalCustomerT extends ExternalCustomer, CustomerT extends Customer> {

    private final ShoppingListRepositoryService shoppingListRepositoryService;
    private final CustomerRepositoryService<CustomerT> customerRepositoryService;
    private final CustomerFactory<CustomerT> customerFactory;
    private final FieldsSynchronisationService<ExternalCustomerT, CustomerT> fieldsSynchronisationService;

    public BaseSynchronisationService(ShoppingListRepository shoppingListRepository, CustomerRepositoryService<CustomerT> customerRepositoryService, CustomerFactory<CustomerT> customerFactory, FieldsSynchronisationService<ExternalCustomerT, CustomerT> fieldsSynchronisationService) {
        this.shoppingListRepositoryService = new ShoppingListRepositoryService(shoppingListRepository);
        this.customerRepositoryService = customerRepositoryService;
        this.customerFactory = customerFactory;
        this.fieldsSynchronisationService = fieldsSynchronisationService;
    }

    SyncResult synchronise(ExternalCustomerT externalCustomer, CustomerMatches<CustomerT> customerMatches) {
        CustomerT customer = customerMatches.getCustomer();

        final SyncResult syncResult;
        if (customer == null) {
            customer = newCustomer(externalCustomer);
            syncResult = SyncResult.CREATED;
        } else {
            syncResult = SyncResult.UPDATED;
        }

        updateFields(externalCustomer, customer);

        customerRepositoryService.createOrUpdate(customer);

        updateDuplicates(externalCustomer, customerMatches);

        return syncResult;
    }

    private void updateDuplicates(ExternalCustomerT externalCustomer, CustomerMatches<CustomerT> customerMatches) {
        for (CustomerT duplicate : customerMatches.getDuplicates()) {
            updateDuplicateName(externalCustomer, duplicate);
        }
    }

    private CustomerT newCustomer(ExternalCustomerT externalCustomer) {
        CustomerT customer;
        if (customerFactory.build() instanceof Person) {
            customer = (CustomerT) new Person();
        } else {
            customer = (CustomerT) new Company();
        }
        customer.setExternalId(externalCustomer.getExternalId());
        customer.setMasterExternalId(externalCustomer.getExternalId());
        return customer;
    }

    private void updateFields(ExternalCustomerT externalCustomer, CustomerT customer) {
        customer.setExternalId(externalCustomer.getExternalId());
        customer.setName(externalCustomer.getName());

        fieldsSynchronisationService.populateFields(externalCustomer, customer);

        updateContactInfo(externalCustomer, customer);
        updateRelations(externalCustomer, customer);
        updatePreferredStore(externalCustomer, customer);
    }

    private void updateRelations(ExternalCustomerT externalCustomer, CustomerT customer) {
        final List<ShoppingList> consumerShoppingLists = externalCustomer.getShoppingLists();
        for (ShoppingList consumerShoppingList : consumerShoppingLists) {
            customer.addShoppingList(consumerShoppingList);
            shoppingListRepositoryService.updateShoppingList(consumerShoppingList);
        }
    }

    private void updateDuplicateName(ExternalCustomerT externalCustomer, CustomerT duplicate) {
        if (duplicate == null) {
            duplicate = newCustomer(externalCustomer);
        }

        duplicate.setName(externalCustomer.getName());

        customerRepositoryService.createOrUpdate(duplicate);
    }

    private void updatePreferredStore(ExternalCustomerT externalCustomer, CustomerT customer) {
        customer.setPreferredStore(externalCustomer.getPreferredStore());
    }

    private void updateContactInfo(ExternalCustomerT externalCustomer, CustomerT customer) {
        customer.setAddress(externalCustomer.getPostalAddress());
    }
}
