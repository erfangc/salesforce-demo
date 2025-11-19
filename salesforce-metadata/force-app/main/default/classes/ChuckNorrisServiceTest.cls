@isTest
private class ChuckNorrisServiceTest {

    /**
     * Test successful API call that returns a joke
     */
    @isTest
    static void testGetRandomJoke_Success() {
        // Set up mock for successful HTTP callout
        Test.setMock(HttpCalloutMock.class, new ChuckNorrisMockSuccess());

        Test.startTest();
        ChuckNorrisService.JokeResponse result = ChuckNorrisService.getRandomJoke();
        Test.stopTest();

        // Verify the response
        System.assertNotEquals(null, result, 'Response should not be null');
        System.assertNotEquals(null, result.value, 'Joke value should not be null');
        System.assertEquals('test-joke-123', result.id, 'Joke ID should match mock');
        System.assertEquals('Chuck Norris can unit test entire applications with a single assert.',
                           result.value,
                           'Joke text should match mock');
        System.assertEquals('https://api.chucknorris.io/img/avatar/chuck-norris.png',
                           result.icon_url,
                           'Icon URL should match mock');
    }

    /**
     * Test failed API call (500 error)
     */
    @isTest
    static void testGetRandomJoke_ServerError() {
        // Set up mock for failed HTTP callout
        Test.setMock(HttpCalloutMock.class, new ChuckNorrisMockServerError());

        Test.startTest();
        Boolean exceptionThrown = false;
        String errorMessage = '';

        try {
            ChuckNorrisService.getRandomJoke();
        } catch (AuraHandledException e) {
            exceptionThrown = true;
            errorMessage = e.getMessage();
        }

        Test.stopTest();

        // Verify exception was thrown
        System.assert(exceptionThrown, 'AuraHandledException should have been thrown');
        System.assert(errorMessage.contains('Unable to fetch Chuck Norris joke'),
                     'Error message should indicate fetch failure');
    }

    /**
     * Test network timeout or connection error
     */
    @isTest
    static void testGetRandomJoke_NetworkError() {
        // Set up mock that throws exception
        Test.setMock(HttpCalloutMock.class, new ChuckNorrisMockNetworkError());

        Test.startTest();
        Boolean exceptionThrown = false;

        try {
            ChuckNorrisService.getRandomJoke();
        } catch (AuraHandledException e) {
            exceptionThrown = true;
        }

        Test.stopTest();

        // Verify exception was thrown
        System.assert(exceptionThrown, 'AuraHandledException should have been thrown for network error');
    }

    /**
     * Test that JokeResponse wrapper class can be instantiated
     * and properties can be set
     */
    @isTest
    static void testJokeResponseWrapper() {
        Test.startTest();

        ChuckNorrisService.JokeResponse joke = new ChuckNorrisService.JokeResponse();
        joke.id = 'test-id';
        joke.value = 'Test joke';
        joke.icon_url = 'https://example.com/icon.png';
        joke.url = 'https://example.com/joke/test-id';
        joke.categories = new List<String>{'dev', 'nerdy'};

        Test.stopTest();

        // Verify properties
        System.assertEquals('test-id', joke.id);
        System.assertEquals('Test joke', joke.value);
        System.assertEquals('https://example.com/icon.png', joke.icon_url);
        System.assertEquals('https://example.com/joke/test-id', joke.url);
        System.assertEquals(2, joke.categories.size());
    }

    // ========== Mock Classes ==========

    /**
     * Mock for successful API response
     */
    private class ChuckNorrisMockSuccess implements HttpCalloutMock {
        public HTTPResponse respond(HTTPRequest req) {
            // Verify the request
            System.assertEquals('GET', req.getMethod(), 'HTTP method should be GET');
            System.assert(req.getEndpoint().contains('api.chucknorris.io'),
                         'Endpoint should be Chuck Norris API');

            // Create successful response
            HttpResponse res = new HttpResponse();
            res.setStatusCode(200);
            res.setStatus('OK');
            res.setHeader('Content-Type', 'application/json');
            res.setBody('{"id":"test-joke-123",' +
                       '"icon_url":"https://api.chucknorris.io/img/avatar/chuck-norris.png",' +
                       '"url":"https://api.chucknorris.io/jokes/test-joke-123",' +
                       '"value":"Chuck Norris can unit test entire applications with a single assert.",' +
                       '"categories":[]}');
            return res;
        }
    }

    /**
     * Mock for server error (500)
     */
    private class ChuckNorrisMockServerError implements HttpCalloutMock {
        public HTTPResponse respond(HTTPRequest req) {
            HttpResponse res = new HttpResponse();
            res.setStatusCode(500);
            res.setStatus('Internal Server Error');
            res.setBody('{"error":"Server error"}');
            return res;
        }
    }

    /**
     * Mock that simulates network error
     */
    private class ChuckNorrisMockNetworkError implements HttpCalloutMock {
        public HTTPResponse respond(HTTPRequest req) {
            // Simulate network error by throwing exception
            throw new CalloutException('Network connection failed');
        }
    }
}
