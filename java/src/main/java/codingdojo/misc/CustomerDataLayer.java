package codingdojo.misc;

public interface CustomerDataLayer {

    Customer upd(Customer customer);

    Customer crt(Customer customer);

    void updateShoppingList(ShoppingList consumerShoppingList);

    Customer find2(String externalId);

    Customer findByMasterExternalId(String externalId);

    Customer find(String companyNumber);
}
