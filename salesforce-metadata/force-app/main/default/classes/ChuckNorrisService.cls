public with sharing class ChuckNorrisService {

    private static final String API_ENDPOINT = 'https://api.chucknorris.io/jokes/random';

    /**
     * Fetches a random Chuck Norris joke from the external API
     * @return JokeResponse wrapper containing the joke text
     */
    @AuraEnabled
    public static JokeResponse getRandomJoke() {
        try {
            // Create HTTP request
            Http http = new Http();
            HttpRequest request = new HttpRequest();
            request.setEndpoint(API_ENDPOINT);
            request.setMethod('GET');
            request.setHeader('Accept', 'application/json');
            request.setTimeout(10000); // 10 second timeout

            // Send request and get response
            HttpResponse response = http.send(request);

            // Check response status
            if (response.getStatusCode() == 200) {
                // Parse JSON response
                String responseBody = response.getBody();
                JokeResponse jokeData = (JokeResponse) JSON.deserialize(
                    responseBody,
                    JokeResponse.class
                );
                return jokeData;
            } else {
                throw new CalloutException(
                    'Failed to fetch joke. Status: ' + response.getStatusCode() +
                    ', Message: ' + response.getStatus()
                );
            }
        } catch (Exception e) {
            // Log error and throw AuraHandledException for LWC
            System.debug('Error in getRandomJoke: ' + e.getMessage());
            throw new AuraHandledException('Unable to fetch Chuck Norris joke: ' + e.getMessage());
        }
    }

    /**
     * Wrapper class for Chuck Norris API response
     * Properties must match JSON response fields
     */
    public class JokeResponse {
        @AuraEnabled public String id;
        @AuraEnabled public String icon_url;
        @AuraEnabled public String url;
        @AuraEnabled public String value; // The actual joke text
        @AuraEnabled public List<String> categories;
    }
}
