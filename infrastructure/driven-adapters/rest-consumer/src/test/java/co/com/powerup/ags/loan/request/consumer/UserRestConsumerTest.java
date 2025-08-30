package co.com.powerup.ags.loan.request.consumer;


import okhttp3.mockwebserver.MockWebServer;


class UserRestConsumerTest {

    private static UserRestConsumer userRestConsumer;

    private static MockWebServer mockBackEnd;


    /*@BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        var webClient = WebClient.builder().baseUrl(mockBackEnd.url("/").toString()).build();
        userRestConsumer = new UserRestConsumer(webClient);
    }

    @AfterAll
    static void tearDown() throws IOException {

        mockBackEnd.shutdown();
    }

    @Test
    @DisplayName("Validate the function testGet.")
    void validateTestGet() {

        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"state\" : \"ok\"}"));
        var response = userRestConsumer.testGet();

        StepVerifier.create(response)
                .expectNextMatches(objectResponse -> objectResponse.getState().equals("ok"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Validate the function testPost.")
    void validateTestPost() {

        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"state\" : \"ok\"}"));
        var response = userRestConsumer.testPost();

        StepVerifier.create(response)
                .expectNextMatches(objectResponse -> objectResponse.getState().equals("ok"))
                .verifyComplete();
    }*/
}