package com.reactnativeshopify;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Iterator;

import org.json.*;

import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.text.Html;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.Arguments;


import com.google.android.gms.wallet.MaskedWalletRequest;
import com.shopify.buy.dataprovider.*;
import com.shopify.buy.model.*;
import com.shopify.buy.utils.AndroidPayHelper;

public class RNShopifyModule extends ReactContextBaseJavaModule {
    
    private final ReactApplicationContext reactContext;
    private BuyClient buyClient;
    private Checkout checkout;
    private List<ShippingRate> availableShippingRates;
    private Promise promise;
    
    public RNShopifyModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }
    
    @Override
    public String getName() {
        return "RNShopify";
    }
    
    @ReactMethod
    public void initialize(String domain, String key, final Promise promise) {
        //Application ID is always 8, as stated in official documentation from Shopify
        buyClient = new BuyClientBuilder()
        .shopDomain(domain)
        .apiKey(key)
        .appId("8")
        .applicationName(getApplicationName())
        .build();
    }
    
    @ReactMethod
    public void getShop(final Promise promise) {
        buyClient.getShop(new Callback<Shop>() {
            @Override
            public void success(Shop shop) {
                try {
                    promise.resolve(convertJsonToMap(new JSONObject(shop.toJsonString())));
                } catch (JSONException e) {
                    promise.reject("", e);
                }
            }
            
            @Override
            public void failure(BuyClientError error) {
                promise.reject("", error.getRetrofitErrorBody());
            }
        });
    }
    
    @ReactMethod
    public void getCollections(int page, final Promise promise) {
        buyClient.getCollections(page, new Callback<List<Collection>>() {
            
            @Override
            public void success(List<Collection> collections) {
                try {
                    WritableArray array = new WritableNativeArray();
                    
                    for(Collection collection : collections) {
                        WritableMap collectionDictionary = convertJsonToMap(new JSONObject(collection.toJsonString()));
                        array.pushMap(collectionDictionary);
                    }
                    
                    promise.resolve(array);
                } catch (JSONException e) {
                    promise.reject("", e);
                }
            }
            
            @Override
            public void failure(BuyClientError error) {
                promise.reject("", error.getRetrofitErrorBody());
            }
        });
    }
    
    @ReactMethod
    public void getProductTags(int page, final Promise promise) {
        buyClient.getProductTags(page, new Callback<List<String>>() {
            
            @Override
            public void success(List<String> tags) {
                WritableArray array = new WritableNativeArray();
                
                for(String tag : tags) {
                    array.pushString(tag);
                }
                
                promise.resolve(array);
            }
            
            @Override
            public void failure(BuyClientError error) {
                promise.reject("", error.getRetrofitErrorBody());
            }
        });
    }
    
    @ReactMethod
    public void getProductsPage(int page, final Promise promise) {
        buyClient.getProducts(page, new Callback<List<Product>>() {
            
            @Override
            public void success(List<Product> products) {
                try {
                    promise.resolve(getProductsAsWritableArray(products));
                } catch (JSONException e) {
                    promise.reject("", e);
                }
            }
            
            @Override
            public void failure(BuyClientError error) {
                promise.reject("", error.getRetrofitErrorBody());
            }
        });
    }
    
    @ReactMethod
    public void getProductsWithTags(int page, ReadableArray tags, final Promise promise) {
        buyClient.getProducts(page, convertReadableArrayToSet(tags), new Callback<List<Product>>() {
            
            @Override
            public void success(List<Product> products) {
                try {
                    promise.resolve(getProductsAsWritableArray(products));
                } catch (JSONException e) {
                    promise.reject("", e);
                }
            }
            
            @Override
            public void failure(BuyClientError error) {
                promise.reject("", error.getRetrofitErrorBody());
            }
        });
    }
    
    @ReactMethod
    public void getProductsWithTagsForCollection(int page, int collectionId, ReadableArray tags,
                                                 final Promise promise) {
        buyClient.getProducts(page, Long.valueOf(collectionId), convertReadableArrayToSet(tags), null,
                              new Callback<List<Product>>() {
                                  
                                  @Override
                                  public void success(List<Product> products) {
                                      try {
                                          promise.resolve(getProductsAsWritableArray(products));
                                      } catch (JSONException e) {
                                          promise.reject("", e);
                                      }
                                  }
                                  
                                  @Override
                                  public void failure(BuyClientError error) {
                                      promise.reject("", error.getRetrofitErrorBody());
                                  }
                              });
    }
    
    /*
     *This 'getProductByHandle:' method exported for getting Product from "handle" String as Parameter
     * */
    
    @ReactMethod
    public void getProductByHandle(String handle, final Promise promise) {
        buyClient.getProductByHandle(handle,new Callback<Product>() {
            
            @Override
            public void success(Product product) {
                try {
                    promise.resolve(getProductAsWritableMap(product));
                } catch (JSONException e) {
                    promise.reject("", e);
                }
            }
            
            @Override
            public void failure(BuyClientError error) {
                promise.reject("", error.getRetrofitErrorBody());
            }
        });
    }
    
    /*
     *This 'getProductById:' method exported for getting Product from its Id
     * */
    @ReactMethod
    public void getProductById(String productId, final Promise promise) {
        try {
            long productNumber = Long.parseLong(productId);
            
            buyClient.getProduct(productNumber, new Callback<Product>() {
                
                @Override
                public void success(Product product) {
                    try {
                        promise.resolve(getProductAsWritableMap(product));
                    } catch (JSONException e) {
                        promise.reject("", e);
                    }
                }
                
                @Override
                public void failure(BuyClientError error) {
                    promise.reject("", error.getRetrofitErrorBody());
                }
            });
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException: " + e.getMessage());
        }
        
    }
    /*
     *This 'getCollectionByHandle:' method exported for getting Collection details from "handle" String as Parameter
     * */
    
    @ReactMethod
    public void getCollectionByHandle(String handle, final Promise promise) {
        buyClient.getCollectionByHandle(handle, new Callback<Collection>() {
            
            @Override
            public void success(Collection collection) {
                try {
                    String description = "";
                    if (collection.getHtmlDescription() != null) {
                        description = getStringFromHTMLString(collection.getHtmlDescription());
                    }
                    //                    WritableMap collectionDictionary = convertJsonToMap(new JSONObject(collection.toJsonString()));
                    WritableMap imageMap = convertJsonToMap(new JSONObject("{}"));
                    if (collection.getImage() == null){
                        imageMap.putString("src", "");
                    }
                    else {
                        imageMap.putString("src", collection.getImage().getSrc());
                    }
                    WritableMap collectionDictionary = convertJsonToMap(new JSONObject("{}"));
                    collectionDictionary.putString("title", collection.getTitle());
                    collectionDictionary.putString("string_description", description);
                    collectionDictionary.putString("collection_id", collection.getCollectionId().toString());
                    collectionDictionary.putString("handle", collection.getHandle());
                    collectionDictionary.putMap("image", imageMap);
                    promise.resolve(collectionDictionary);
                } catch (JSONException e) {
                    promise.reject("", e);
                }
            }
            
            @Override
            public void failure(BuyClientError error) {
                
            }
        });
    }
    
    /*
     * This 'getCollectionById:' method exported for getting Collections from from its Id
     * */
    @ReactMethod
    public void getCollectionById(final String collectionId, final Promise promise) {
        
        ArrayList<Long> collectionIds = new ArrayList<Long>();
        try {
            long collectionNumber = Long.parseLong(collectionId);
            collectionIds.add(collectionNumber);
            buyClient.getCollections(1, collectionIds, new Callback<List<Collection>>() {
                
                @Override
                public void success(List<Collection> response) {
                    Collection collection = response.get(0);
                    try {
                        String description = "";
                        if (collection.getHtmlDescription() != null) {
                            description = getStringFromHTMLString(collection.getHtmlDescription());
                        }
                        //                        WritableMap collectionDictionary = convertJsonToMap(new JSONObject(collection.toJsonString()));
                        WritableMap imageMap = convertJsonToMap(new JSONObject("{}"));
                        if (collection.getImage() == null){
                            imageMap.putString("src", "");
                        }
                        else {
                            imageMap.putString("src", collection.getImage().getSrc());
                        }
                        WritableMap collectionDictionary = convertJsonToMap(new JSONObject("{}"));
                        collectionDictionary.putString("title", collection.getTitle());
                        collectionDictionary.putString("string_description", description);
                        collectionDictionary.putString("collection_id", collection.getCollectionId().toString());
                        collectionDictionary.putString("handle", collection.getHandle());
                        collectionDictionary.putMap("image", imageMap);
                        promise.resolve(collectionDictionary);
                    } catch (JSONException e) {
                        promise.reject("", e);
                    }
                }
                
                @Override
                public void failure(BuyClientError error) {
                    
                }
            });
            
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException: " + e.getMessage());
        }
        
    }
    
    /*
     * Get string from Html String
     * */
    public String getStringFromHTMLString(String html) {
        return Html.fromHtml(html).toString();
    }
    
    @ReactMethod
    public void checkout(ReadableArray cartItems, final Promise promise) {
        Cart cart;
        
        try {
            cart = new Cart();
            for (int i = 0; i < cartItems.size(); i++) {
                ReadableMap cartItem = cartItems.getMap(i);
                ReadableMap variantDictionary = cartItem.getMap("variant");
                int quantity = cartItem.getInt("quantity");
                
                JSONObject variantAsJsonObject = convertMapToJson(variantDictionary);
                ProductVariant variant = fromVariantJson(variantAsJsonObject.toString());
                
                for(int j = 0; j < quantity; j++) {
                    cart.addVariant(variant);
                }
            }
        } catch (JSONException e) {
            promise.reject("", e);
            return;
        }
        
        checkout = new Checkout(cart);
        
        // Sync the checkout with Shopify
        buyClient.createCheckout(checkout, new Callback<Checkout>() {
            
            @Override
            public void success(Checkout checkout) {
                RNShopifyModule.this.checkout = checkout;
                promise.resolve(true);
            }
            
            @Override
            public void failure(BuyClientError error) {
                promise.reject("", error.getRetrofitErrorBody());
            }
        });
    }
    
    @ReactMethod
    public void setCustomerInformation(String email, ReadableMap addressDictionary, final Promise promise) {
        try {
            String addressAsJson = convertMapToJson(addressDictionary).toString();
            Address address = fromAddressJson(addressAsJson);
            address.setLastName(addressDictionary.getString("lastName"));
            address.setCountryCode(addressDictionary.getString("countryCode"));
            checkout.setEmail(email);
            checkout.setShippingAddress(address);
            checkout.setBillingAddress(address);
        } catch (JSONException e) {
            promise.reject("", e);
            return;
        }
        
        buyClient.updateCheckout(checkout, new Callback<Checkout>() {
            
            @Override
            public void success(Checkout checkout) {
                RNShopifyModule.this.checkout = checkout;
                promise.resolve(true);
            }
            
            @Override
            public void failure(BuyClientError error) {
                promise.reject("", error.getRetrofitErrorBody());
            }
        });
    }
    
    @ReactMethod
    public void getShippingRates(final Promise promise) {
        buyClient.getShippingRates(checkout.getToken(), new Callback<List<ShippingRate>>() {
            
            @Override
            public void success(List<ShippingRate> shippingRates) {
                RNShopifyModule.this.availableShippingRates = shippingRates;
                try {
                    promise.resolve(getShippingRatesAsWritableArray(shippingRates));
                } catch (JSONException e) {
                    promise.reject("", e);
                    return;
                }
            }
            
            @Override
            public void failure(BuyClientError error) {
                promise.reject("", error.getRetrofitErrorBody());
            }
        });
    }
    
    @ReactMethod
    public void selectShippingRate(int shippingRateIndex, final Promise promise) {
        ShippingRate selectedShippingRate = availableShippingRates.get(shippingRateIndex);
        checkout.setShippingRate(selectedShippingRate);
        
        // Update the checkout with the shipping rate
        buyClient.updateCheckout(checkout, new Callback<Checkout>() {
            @Override
            public void success(Checkout checkout) {
                RNShopifyModule.this.checkout = checkout;
                promise.resolve(true);
            }
            
            @Override
            public void failure(BuyClientError error) {
                promise.reject("", error.getRetrofitErrorBody());
            }
        });
    }
    
    @ReactMethod
    public void completeCheckout(ReadableMap cardDictionary, final Promise promise) {
        CreditCard card = new CreditCard();
        card.setNumber(cardDictionary.getString("number"));
        card.setFirstName(cardDictionary.getString("firstName"));
        card.setLastName(cardDictionary.getString("lastName"));
        card.setMonth(cardDictionary.getString("expiryMonth"));
        card.setYear(cardDictionary.getString("expiryYear"));
        card.setVerificationValue(cardDictionary.getString("cvv"));
        
        // Associate the credit card with the checkout
        buyClient.storeCreditCard(card, checkout, new Callback<PaymentToken>() {
            @Override
            public void success(PaymentToken paymentToken) {
                buyClient.completeCheckout(paymentToken, checkout.getToken(), new Callback<Checkout>() {
                    @Override
                    public void success(Checkout returnedCheckout) {
                        promise.resolve(true);
                    }
                    
                    @Override
                    public void failure(BuyClientError error) {
                        promise.reject("", error.getRetrofitErrorBody());
                    }
                });
            }
            
            @Override
            public void failure(BuyClientError error) {
                promise.reject("", error.getRetrofitErrorBody());
            }
        });
    }
    
    @ReactMethod
    public void webCheckout(ReadableArray cartItems, final Promise promise) {
        Cart cart;
        this.promise = promise;
        try {
            cart = new Cart();
            for (int i = 0; i < cartItems.size(); i++) {
                ReadableMap cartItem = cartItems.getMap(i);
                ReadableMap variantDictionary = cartItem.getMap("variant");
                int quantity = cartItem.getInt("quantity");
                
                JSONObject variantAsJsonObject = convertMapToJson(variantDictionary);
                ProductVariant variant = fromVariantJson(variantAsJsonObject.toString());
                
                for(int j = 0; j < quantity; j++) {
                    cart.addVariant(variant);
                }
            }
        } catch (JSONException e) {
            promise.reject("", e);
            return;
        }
        checkout = new Checkout(cart);
        
        if (checkout != null) {
            
            buyClient.createCheckout(checkout, new Callback<Checkout>() {
                
                @Override
                public void success(Checkout checkout) {
                    RNShopifyModule.this.checkout = checkout;
                    
                    Context context = RNShopifyModule.this.reactContext.getApplicationContext();
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                    browserIntent.setData(Uri.parse(checkout.getWebUrl()));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        context.startActivity(browserIntent);
                        RNShopifyModule.this.promise.resolve(true);
                    } catch (ActivityNotFoundException e){
                        RNShopifyModule.this.promise.reject("10001","No browser available", e);
                    }
                }
                
                @Override
                public void failure(BuyClientError error) {
                    RNShopifyModule.this.promise.reject(error.getRetrofitErrorBody(), error.getRetrofitErrorBody(), error);
                }
            });
        }
        else {
            promise.reject("Error", "Please checkout ");
        }
    }
    
    private WritableArray getProductsAsWritableArray(List<Product> products) throws JSONException {
        
        WritableArray array = new WritableNativeArray();
        for (Product product : products) {
            array.pushMap(getProductAsWritableMap(product));
        }
        return array;
    }
    
    /*
     *This 'getProductAsWritableMap:' method created for parsing BUYProduct Object and return WritableMap
     * */
    
    private WritableMap getProductAsWritableMap(Product product) throws JSONException {
        WritableMap productMap = convertJsonToMap(new JSONObject(product.toJsonString()));
        productMap.putString("minimum_price", product.getMinimumPrice());
        productMap.putString("string_description", getStringFromHTMLString(product.getBodyHtml()));
        return productMap;
    }
    
    
    private WritableArray getShippingRatesAsWritableArray(List<ShippingRate> shippingRates) throws JSONException {
        WritableArray result = new WritableNativeArray();
        
        for (ShippingRate shippingRate : shippingRates) {
            WritableMap shippingRateMap = convertJsonToMap(new JSONObject(toJsonString(shippingRate)));
            
            if(shippingRate.getDeliveryRangeDates() != null) {
                WritableArray deliveryDatesInMiliseconds = new WritableNativeArray();
                
                for(Date deliveryDate : shippingRate.getDeliveryRangeDates()) {
                    deliveryDatesInMiliseconds.pushDouble(deliveryDate.getTime());
                }
                shippingRateMap.putArray("deliveryDates", deliveryDatesInMiliseconds);
            }
            result.pushMap(shippingRateMap);
        }
        
        return result;
    }
    
    private Set<String> convertReadableArrayToSet(ReadableArray array) {
        Set<String> set = new HashSet<String>();
        
        for (int i = 0; i < array.size(); i++) {
            set.add(array.getString(i));
        }
        
        return set;
    }
    
    private WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
        WritableMap map = new WritableNativeMap();
        
        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.putArray(key, convertJsonToArray((JSONArray) value));
                if(("option_values").equals(key)) {
                    map.putArray("options", convertJsonToArray((JSONArray) value));
                }
            } else if (value instanceof Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String)  {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }
    
    private WritableArray convertJsonToArray(JSONArray jsonArray) throws JSONException {
        WritableArray array = new WritableNativeArray();
        
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                array.pushMap(convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                array.pushArray(convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                array.pushBoolean((Boolean) value);
            } else if (value instanceof Integer) {
                array.pushInt((Integer) value);
            } else if (value instanceof Double) {
                array.pushDouble((Double) value);
            } else if (value instanceof String)  {
                array.pushString((String) value);
            } else {
                array.pushString(value.toString());
            }
        }
        return array;
    }
    
    private JSONObject convertMapToJson(ReadableMap readableMap) throws JSONException {
        JSONObject object = new JSONObject();
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            switch (readableMap.getType(key)) {
                case Null:
                    object.put(key, JSONObject.NULL);
                    break;
                case Boolean:
                    object.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    object.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    object.put(key, readableMap.getString(key));
                    break;
                case Map:
                    object.put(key, convertMapToJson(readableMap.getMap(key)));
                    break;
                case Array:
                    object.put(key, convertArrayToJson(readableMap.getArray(key)));
                    break;
            }
        }
        return object;
    }
    
    private JSONArray convertArrayToJson(ReadableArray readableArray) throws JSONException {
        
        JSONArray array = new JSONArray();
        
        for (int i = 0; i < readableArray.size(); i++) {
            switch (readableArray.getType(i)) {
                case Null:
                    break;
                case Boolean:
                    array.put(readableArray.getBoolean(i));
                    break;
                case Number:
                    array.put(readableArray.getDouble(i));
                    break;
                case String:
                    array.put(readableArray.getString(i));
                    break;
                case Map:
                    array.put(convertMapToJson(readableArray.getMap(i)));
                    break;
                case Array:
                    array.put(convertArrayToJson(readableArray.getArray(i)));
                    break;
            }
        }
        return array;
    }
    
    private ProductVariant fromVariantJson(String json) {
        return BuyClientUtils.createDefaultGson().fromJson(json, ProductVariant.class);
    }
    
    private Address fromAddressJson(String json) {
        return BuyClientUtils.createDefaultGson().fromJson(json, Address.class);
    }
    
    private String toJsonString(Object object) {
        return BuyClientUtils.createDefaultGson().toJson(object);
    }
    
    private String getApplicationName() {
        Context context = this.reactContext.getApplicationContext();
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }
}
