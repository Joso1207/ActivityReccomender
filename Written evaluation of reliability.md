# 1k5: AI integration, Evaluation of Reliability

## Prompt Strategy
The prompting strategy has been shown to be of utmost important when crafting a system prompt as the AI is prone to hallucination of not just the data,
but the other parametres its been given.

One such example was that it was prone to give weather warnings about the swedish weather when none were issued, At times even snow in the middle of june.
It also frequently gave reccomendations which it was not allowed to, making it even more important to tighten the rules.

Over all,  The important takeaways is that a system prompt needs to include 5 things to maintain reliability and avoid user input from tainting it.
- 1. Describe what it needs to do, Preferably in concise programming terminology (Calling it to output DTO for example)
- 2. Define what valid outputs looks like,  in this case actual JSON format with the proper fields and datatypes.
- 3. Define each field within the JSON, including the length of textfields and limiting the lengths of lists, The AI needs to understand what each field represents ot it will hallucinate meaning that isnt there.
- 4. RULES! a list of absolute constraints that the AI is not allowed to move outside.  Currently each rule within the AIClientService class is one that the AI has broken.
- 5. The bounds of each field.  In this case the acceptable categories for GeoApify

# Exception and Error Handling

The strategies and design decision to ensure proper management of volitile external data.

## Hidden Variable Dependencies.   
The application is dependent on API Keys.  Such keys are not meant to be within human readable code but rather are stored away externally or within system variables, which Spring can read through @Value() and ${APiKEY}
By utilising @PostConstruct on our APIs we can ensure that a non-resolved variable causes an instant crash in a 'FailFast-Fashion' with a proper messsage as to why.

## Timeouts
AI computations are heavy.  Therefore we want to ensure an upper bound on connection and read time.  In this strategy we define a spring @Configuration for our RestClientFactory as a bean. Which we can modify through profiles and yml
```
@Bean
    RestClient openAiRestClient(
            @Value("${openai.timeouts.read}") int readTimeout,
            @Value("${openai.timeouts.connect}") int connectTimeout) {

        var factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
```

This causes a ResourceAccessException upon exceeding the defined durations which we can catch in a try-catch block, However since this is considered a transient error we simply log it and retry with an exponential backoff.
First retry 1000ms,  second 2000ms, third and last 4000ms,

## Rate-Limits
The rest client runs through a for loop within a try-catch block above.   So naturally its not just ResourceExceptions we catch, But most importantly we read the Response HTTPStatusCode.  For transient errors we log and continue, for others we rethrow to exit the loop.
For the issue of Ratelimits we expect to recieve a 429 response from the API, an Error Code. Which much like ResourceAccessExceptions are considered transient errors.
Leading to the same retry loop defined above which doubles the delay after each successive attempt until we simply give up and throw an exception explaining that we ran out of Retries.

## ResponseValidation
The response validation is handled through the Tools we recieve from Jackson's Objectmapper and Jackarta's Validator with some BeanValidation annotations.
Starting with the object mapper it will throw a wide series of Exceptions all grouped within the JacksonException parent class. 
So when we recieve a 200 response we first want to ensure it is of proper Json format and use ObjectMapper to convert it to JsonNode.  Any issues here will produce a stream read exception, but we can catch all JacksonExceptions to catch other issues aswell.
Second we move onto parsing what we have into a valid DTO response,  if the fields we recieved earlier are wrong or of wrong type we get a databind exception here.

In either case we return an Ai fallback response.  However in the latter we can give more information about what is wrong which becomes the AI summary.
We also use the fallback when the AI reports itself as having low confidence (less than 60%)

## Other Issues
Following up on Retries and what constitutes a transient error. Using an old HTTP Joke.
  - 1XX - Hold on
  - 2XX - Here you go
  - 3XX - Go Away
  - 4XX - You Fucked up
  - 5XX - I Fucked up.

We can reasonably assume that errors not on the users or our part are safe to retry.  So we engage the same retry policy on 5XX errors as describes above. 
We do not however handle 3XX or 1XX errors as they hit the retry loop by default. We might want to handle them in a production setting but we do not need to here. But we cannot retry 4XX errors with exception of 429, As those are typically from user error like bad input and thus theres no point in retrying it.

## Ai Criticism
AI seems to be able to produce reliable output when proper definitions and fences are in place.  However it seems like even with these its rather prone to hallucinations.
I have yet to do any testing where a user is attempting to break the system prompt but i am sure this is possible with the right selection of words as I am not aware of any priority in orders.
This can have great ramifications within a production setting if it needs to deliver reliable data and the occasional even if unlikely exception is ultimately unacceptable.

While I believe I have done what is needed to make a safe application with it, 
by minimizing the errors and I also have to admit that the risk of getting wrong weather info is neglible and acceptable considering the typical Meteorological sources already can be iffy.

Would I use it with anything financial, or something akin to an AI cookbook?   Absolutely not as theres still the risk of it making horrible errors disguised as good data.


