package travelling.with.code.restful.phonebook.tests;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.context.WebApplicationContext;

import net.minidev.json.JSONObject;
import travelling.with.code.restful.phonebook.Application;
import travelling.with.code.restful.phonebook.resources.IndexedContact;
import travelling.with.code.restful.phonebook.resources.InMemoryPhoneBook;
import travelling.with.code.restful.phonebook.resources.PhoneBook;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
//@WebIntegrationTest // Use for end-to-end tests (TestRestTemplate)
public class ServerSideTests {

    private static final String phoneBookUrl = "http://localhost:8080/phonebook/";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PhoneBook phoneBook;

    /**
     * The contacts that will be used to fill the phonebook and run the tests.
     */
    private ArrayList<IndexedContact> contacts;

    /**
     * The same contacts as {@link ServerSideTests#contacts}, but in {@link JSONObject} form.
     * {@link JSONObject} belongs to the Json Smart project, which Jayway Jsonpath uses by default as a Json Provider.
     * These contacts will be used as an input to {@link MockMvcResultMatchers} methods to test if the controller's response
     * is the one expected.
     */
    private ArrayList<JSONObject> jsonContacts;

    private MockMvc mockMvc;


//    private RestTemplate testTemplate;

    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));


    @Before
    public void setup() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
//        testTemplate = new TestRestTemplate();    // Use for end-to-end tests

        if (phoneBook instanceof InMemoryPhoneBook) {
            ResourcesContactsFactory contactsFactory = new ResourcesContactsFactory();
            ((InMemoryPhoneBook) phoneBook).setContactsFactory(contactsFactory);
            ((InMemoryPhoneBook) phoneBook).init();	// FIXME
            contacts = new ArrayList<>(contactsFactory.createContactsCollection());
            jsonContacts = new ArrayList<>(contacts.stream().map(this::createJsonObject).collect(Collectors.toList()));
        }
    }

    private JSONObject createJsonObject(IndexedContact contact) {
        return createJsonObject(contact.getId(), contact.getName(), contact.getSurname(), contact.getPhone());
    }

    private JSONObject createJsonObject(long id, String name, String surname, String phone) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("phone", phone);
        jsonObject.put("surname", surname);
        jsonObject.put("name", name);
        jsonObject.put("id", id);
        return jsonObject;
    }

    @Test
    public void getAllContacts() throws Exception {
        mockMvc.perform(get(phoneBookUrl + "contacts").accept(contentType))
               .andExpect(status().isOk())
               .andExpect(content().contentType(contentType))
               .andDo(result -> {System.out.println("Response: " + result.getResponse().getContentAsString());})
               .andExpect(jsonPath("$", hasSize(jsonContacts.size())))
               .andExpect(result -> jsonContacts.stream().forEach(contact -> jsonPath("$", hasItem(contact))));
    }

    @Test
    public void findContactById() throws Exception {
        IndexedContact randomContact = getRandomContact();
        mockMvc.perform(get(phoneBookUrl + "contacts/{id}", randomContact.getId()).accept(contentType))
               .andExpect(status().isOk())
               .andExpect(content().contentType(contentType))
               .andDo(result -> {System.out.println("Response: " + result.getResponse().getContentAsString());})
               .andExpect(jsonPath("$.id", is(Integer.valueOf(randomContact.getId().toString()))))
               .andExpect(jsonPath("$.name", is(randomContact.getName())))
               .andExpect(jsonPath("$.surname", is(randomContact.getSurname())))
               .andExpect(jsonPath("$.phone", is(randomContact.getPhone())));
    }

    @Test
    public void findNotExistingContactById() throws Exception {
        mockMvc.perform(get(phoneBookUrl + "/contacts/{id}", contacts.size() + 1).accept(contentType))
                    .andDo(result -> {System.out.println("Response: " + result.getResponse().getContentAsString());})
                    .andExpect(status().isNotFound());
    }

    @Test
    public void findContactsByName() throws Exception {
        IndexedContact randomContact = getRandomContact();
        mockMvc.perform(get(phoneBookUrl + "/contacts?name={name}", randomContact.getName()).accept(contentType))
               .andExpect(status().isOk())
               .andExpect(content().contentType(contentType))
               .andDo(result -> {System.out.println("Response: " + result.getResponse().getContentAsString());})
               .andExpect(jsonPath("$.[*].name", everyItem(is(randomContact.getName()))));
    }

    @Test
    public void findContactsByNotExistingName() throws Exception {
        String impropableNameToApper = "asdfghjkqwertqewtqadfafdlkhouwe";
        mockMvc.perform(get(phoneBookUrl + "/contacts?name={name}", impropableNameToApper).accept(contentType))
                .andDo(result -> {System.out.println("Response: " + result.getResponse().getContentAsString());})
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void findContactsBySurname() throws Exception {
        IndexedContact randomContact = getRandomContact();
        mockMvc.perform(get(phoneBookUrl + "/contacts?surname={surname}", randomContact.getSurname()).accept(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andDo(result -> {System.out.println("Response: " + result.getResponse().getContentAsString());})
                .andExpect(jsonPath("$.[*].surname", everyItem(is(randomContact.getSurname()))));
    }

    @Test
    public void findContactsByNameAndSurname() throws Exception {
        IndexedContact randomContact = getRandomContact();
        mockMvc.perform(get(phoneBookUrl + "/contacts?name={name}&surname={surname}", randomContact.getName(), randomContact.getSurname()).accept(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andDo(result -> {System.out.println("Response: " + result.getResponse().getContentAsString());})
                .andExpect(jsonPath("$.[*].name", everyItem(is(randomContact.getName()))))
                .andExpect(jsonPath("$.[*].surname", everyItem(is(randomContact.getSurname()))));
    }

    @Test
    public void findContactsByPhone() throws Exception {
        IndexedContact randomContact = getRandomContact();
        mockMvc.perform(get(phoneBookUrl + "/contacts?phone={phone}", randomContact.getPhone()).accept(contentType))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andDo(result -> {System.out.println("Response: " + result.getResponse().getContentAsString());})
                .andExpect(jsonPath("$.[*].phone", everyItem(is(randomContact.getPhone()))));
    }

    private IndexedContact getRandomContact() {
        Random randomIndexGenerator = new Random();
        int randomContactIndex = randomIndexGenerator.nextInt(contacts.size());
        return contacts.get(randomContactIndex);
    }

}
