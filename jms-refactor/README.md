## Refactoring Kata
You will find here an implementation that supports the sending and receiving of messages from an Apache 
Active MQ queue broker.  This class was written to simplify sending and receiving messages during automated 
system level testing.  The core use case is:

  - deploy the application
  - start the application
  - test the application by sending messages and asserting received messages
  
A typical use in a test might look something like:

    @Test
    public void systemProcessesTradesAndCreatesAccountRequests(){
    
        String accountingMessage = bindToBrokerAtUrl(url)
            .andThen().sendATextMessageToDestinationAt(inputQueue, incomingTradeMessage)
            .andThen().retrieveASingleMessageFromTheDestination(accountingQueue);
        assertThat(accountingMessage).isEqualTo(expectedAccountingRequestMessage);
    }

At first glance this looks like a nice API, and it satisfies the initial problem.  In fact its so good we want to use
this API as the main API for all of the application messaging (not just for our testing).  When we consider the new use case and we consider the
SOLID principles we can see a few problems.  Some thoughts:

  - if I wanted to use a different broker technology the coupling here is tight (OCP)
  - if I considered that classes should only do one thing (SRP) I can see a lot of responsibilities in the classes

If I wanted a fluid API I would probably want to say something like:

    @Test
    public void blah() {
        bindToActiveMqBrokerAt(url)
            .and().sendTheMessage(inputMessage).to(inputQueue)
            .andThen().waitForAMessageOn(accountingQueue);
            
        bindToIbmMqBrokerAt(url)
            .and().sendTheMessage(inputMessage).to(inputQueue)
            .andThen().waitForAMessageOn(accountingQueue);
            
        bindToTibcoMqBrokerAt(url)
            .and().sendTheMessage(inputMessage).to(inputQueue)
            .andThen().waitForAMessageOn(accountingQueue);
    }

In the meantime we need to consider backwards compatibility of the utility: imagine a large number of hard to contact 
consumers of these classes who would want to continue using the existing API (albeit in a deprecated state).

## The challenge
Understand the API and consider how we use it in the testing context.  Consider the use of this in application code (we use a 
dependency injection framework, if its at all relevant to you).  Please refactor this implementation taking into account the above.

Once you have finished and think this is good enough for consideration in the production environment, please send us back 
the implementation plus any comments or thoughts you have for this.  If your submission is strong enough we will invite you
to come and meet us and we can discuss your thoughts in a pair code review.

We look forward to receiving your submission.

