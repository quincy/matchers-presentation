---
title: Beyond JUnit--Testing With Hamcrest Matchers
theme: league
verticalSeparator: "^\n\n"
---
# Beyond JUnit
## Testing With Hamcrest Matchers
### Quincy Bowers

#### Software Developer Team Lead
#### Clearwater Analytics

---

![Collaborators](res/orly.png)
<!-- .element style="width: 45%" -->

---

# Agenda
* The goals of testing
* A brief discussion on Test Doubles
* Given, When, Then testing
* Improving a typical test
* What is a Matcher?
* Implementing the Matcher interface
* Existing Matchers

Note: Here is a brief overview of what we're going to talk about today

---

# The Goals of Testing


## Verification
Prove the problem is solved


## Prevention
Never fix the same bug twice


## Implementation
Guides your design toward loosely coupled components

Punishes you when your classes are too big or contain multiple responsibilities
<!-- .element: class="fragment" data-fragment-index="1" -->

If it's painful to test, it'll be painful to debug.
<!-- .element: class="fragment" data-fragment-index="2" -->


## Documentation
Documents how the code is meant to be used

---

# Test Doubles
## Stubs, Mocks, and Fakes


## Stubs

A stub has no logic

It simply returns what it is told to


## Stub example
    Authenticator authenticator = new Authenticator(stubAuthenticationService);
    
    when(stubAuthenticationService.isAuthenticated(userId)).thenReturn(false);
    assertThat(authenticator.userHasAccess(userId), is(false));
    
    when(stubAuthenticationService.isAuthenticated(userId)).thenReturn(true);
    assertThat(authenticator.userHasAccess(userId), is(true));
<!-- .element style="font-size: 0.5em; width: 100%;" -->


# Mocks
A mock is an object that keeps track of how it has been interacted with

They help you to verify that the interactions between objects are happening as expected


## Mock example
    Authenticator authenticator = new Authenticator(mockAuthenticationService);
    authenticator.userHasAccess(userId);
    
    verify(mockAuthenticationService).isAuthenticated(userId);
<!-- .element style="font-size: 0.5em; width: 100%;" -->


## Fakes
A fake is a real object that implements the same API as another object

If it would be awkward to use the real implementation in your test, consider writing a fake


## Fake example
    AuthenticationService fakeAuthenticationService
            = new FakeAuthenticationService();
    Authenticator authenticator = new Authenticator(fakeAuthenticationService);
    
    assertThat(authenticator.userHasAccess(userId), is(false));
    
    fakeAuthenticationService.addAuthenticatedUser(userId);
    assertThat(authenticator.userHasAccess(userId), is(true));
<!-- .element style="font-size: 0.5em; width: 100%;" -->


## Mocking Frameworks
You've probably used Mockito, or some other mocking framework

Here are some rules to avoid misusing them
<!-- .element: class="fragment" data-fragment-index="1" -->


## Rules To Mock By
Mock behavior, not data

Don't mock out the getters on a POJO, just make an instance
<!-- .element: class="fragment" data-fragment-index="1" -->

Builders can help
<!-- .element: class="fragment" data-fragment-index="2" -->


## Rules To Mock By
Mock collaborators (dependencies)

Don't mock the object under test
<!-- .element: class="fragment" data-fragment-index="1" -->

You should only be interested in testing one unit at a time
<!-- .element: class="fragment" data-fragment-index="2" -->

Note: Collaborators are the objects that your object under test interacts with in order to do its job

They are the things you should be injecting into your object


![Collaborators](res/class-diagram.png)
<!-- .element style="width: 150%" -->


## Rules To Mock By
Never mock something you don't own

This leads to brittle tests
<!-- .element: class="fragment" data-fragment-index="1" -->

The interface can change without warning when you update your dependencies
<!-- .element: class="fragment" data-fragment-index="2" -->

Put your own interface in front of those objects and mock that instead
<!-- .element: class="fragment" data-fragment-index="3" -->


## Rules To Mock By
Only verify state changing methods

Performing verification on every method call leads to brittle tests
<!-- .element: class="fragment" data-fragment-index="1" -->

Only verify the methods which change the state of the system you are testing
<!-- .element: class="fragment" data-fragment-index="2" -->

---

# Given, When, Then


Given some context


When some action is carried out


Then a particular set of observable consequences should occur

Note: Specification by Example


## Why Given, When, Then?
Focuses on behavior
<!-- .element: class="fragment" data-fragment-index="1" -->

Leads to more robust tests
<!-- .element: class="fragment" data-fragment-index="2" -->

Transforms tests into documentation
<!-- .element: class="fragment" data-fragment-index="3" -->


## Test Specification
    Feature: Portfolio trades stocks
      Scenario: Portfolio requests a sell before close of trading
     
      Given I have 100 shares of MSFT stock
        And I have 150 shares of APPL stock
        And the time is before close of trading
     
      When I ask to sell 20 shares of MSFT stock
     
      Then I should have 80 shares of MSFT stock
        And I should have 150 shares of APPL stock
        And a sell order for 20 shares of MSFT stock should have
          been executed

---

# The Code


# Portfolio
    public class Portfolio {
        private final Map<String, Position> positions;
        private final TradeClock clock;
        private final MarketDao marketDao;

        public Portfolio(Map<String, Position> positions, TradeClock clock, MarketDao marketDao) {
            this.positions = positions;
            this.clock = clock;
            this.marketDao = marketDao;
        }

        public Optional<Position> getPosition(String symbol) {
            return Optional.ofNullable(positions.get(symbol));
        }

        public Set<Position> getPositions() {
            return new HashSet<>(positions.values());
        }

        public Transaction trade(Trade order) throws MarketClosedException {
            if (clock.isMarketOpen()) {
                Transaction transaction = marketDao.execute(order);
                applyTransaction(transaction);
                return transaction;
            }

            throw new MarketClosedException();
        }

        private void applyTransaction(Transaction transaction) {
            positions.compute(transaction.getSymbol(),
                    (symbol, oldPosition)
                            -> new Position(symbol, oldPosition.getUnits()
                                    + transaction.getUnitsAdjustment()));
        }
    }
<!-- .element style="font-size: 0.43em; width: 100%;" -->


# TradeClock
Determines if the market is open or not

    @FunctionalInterface
    public interface TradeClock {
        boolean isMarketOpen();
    }


# MarketDao
Executes trades on the stock market

    public class MarketDao {
        public Transaction execute(Trade trade) {
            // Pretend this actually executed a trade on the stock market
            return new Transaction(trade);
        }
    }
<!-- .element style="font-size: 0.5em; width: 100%;" -->

---

# The Test


## Typical Test v1
    @Test
    public void userTradesStocks_v1() throws MarketClosedException {
        Portfolio portfolio = new Portfolio.Builder()
                // Given I have 100 shares of MSFT stock
                .withPosition(new Position("MSFT", 100.0))
                //   And I have 150 shares of APPL stock
                .withPosition(new Position("APPL", 150.0))
                //   And the time is before close of trading
                .withTradeClock(new Clock())
                .withMarketDao(new MarketDao())
                .build();

        // When I ask to sell 20 shares of MSFT stock
        Trade sellOrder = new SellOrder("MSFT", 20.0);
        portfolio.trade(sellOrder);

        // Then I should have 80 shares of MSFT stock
        assertEquals(new Position("MSFT", 80.0), portfolio.getPosition("MSFT").orElse(null));
        //   And I should have 150 shares of APPL stock
        assertEquals(new Position("APPL", 150.0), portfolio.getPosition("APPL").orElse(null));

        // This last part must have happened right?
        //   And a sell order for 20 shares of MSFT stock should have been executed
    }
<!-- .element style="font-size: 0.43em; width: 100%;" -->


## Problems With v1
    Portfolio portfolio = new Portfolio.Builder()
            // Given I have 100 shares of MSFT stock
            .withPosition(new Position("MSFT", 100.0))
            //   And I have 150 shares of APPL stock
            .withPosition(new Position("APPL", 150.0))
            //   And the time is before close of trading
            .withTradeClock(new Clock())     // <-- look here
            .withMarketDao(new MarketDao())  // <-- look here
            .build();

It's a LARGE test!
<!-- .element: class="fragment" data-fragment-index="1" -->

The test attempts to execute real trades
<!-- .element: class="fragment" data-fragment-index="2" -->

The test fails if ran after hours
<!-- .element: class="fragment" data-fragment-index="3" -->

---

## Improved Test v2
    // Use a mock MarketDao to avoid real trades being executed from our test!
    MarketDao mockMarketDao = mock(MarketDao.class);
    when(mockMarketDao.execute(sellOrder)).thenReturn(new Transaction(sellOrder));

    // Use a mock TradeClock so we can control its behavior and run our test at
    // any time of day.
    TradeClock mockClock = mock(TradeClock.class);
    when(mockClock.isMarketOpen()).thenReturn(true);

    Portfolio portfolio = new Portfolio.Builder()
            // Given I have 100 shares of MSFT stock
            .withPosition(new Position("MSFT", 100.0))
            //   And I have 150 shares of APPL stock
            .withPosition(new Position("APPL", 150.0))
            //   And the time is before close of trading
            .withTradeClock(mockClock)
            .withMarketDao(mockMarketDao)
            .build();
<!-- .element style="font-size: 0.5em; width: 100%;" -->


But we can still improve things

    // This last part must have happened right?
    //   And a sell order for 20 shares of MSFT stock should have been executed
<!-- .element style="font-size: 0.5em; width: 100%;" -->

---

## Improved Test v3
We can verify this expected behavior...

    // This last part must have happened right?
    //   And a sell order for 20 shares of MSFT stock should have been executed
<!-- .element style="font-size: 0.5em; width: 100%;" -->

By using the Mockito verify method <!-- .element: class="fragment" data-fragment-index="1" -->

    //   And a sell order for 20 shares of MSFT stock should have been executed
    verify(mockMarketDao).execute(sellOrder);
<!-- .element: class="fragment" data-fragment-index="1" -->
<!-- .element style="font-size: 0.5em; width: 100%;" -->

---

## Good Enough?
    @Test
    public void userTradesStocks_v3() throws MarketClosedException {
        Trade sellOrder = new SellOrder("MSFT", 20.0);
        MarketDao mockMarketDao = mock(MarketDao.class);
        when(mockMarketDao.execute(sellOrder)).thenReturn(new Transaction(sellOrder));
        TradeClock mockClock = mock(TradeClock.class);
        when(mockClock.isMarketOpen()).thenReturn(true);
        Portfolio portfolio = new Portfolio.Builder()
                .withPosition(new Position("MSFT", 100.0))
                .withPosition(new Position("APPL", 150.0))
                .withTradeClock(mockClock)
                .withMarketDao(mockMarketDao)
                .build();
        portfolio.trade(sellOrder);

        // Then I should have 80 shares of MSFT stock
        assertEquals(new Position("MSFT", 80.0),
                portfolio.getPosition("MSFT").orElse(null));
        //   And I should have 150 shares of APPL stock
        assertEquals(new Position("APPL", 150.0),
                portfolio.getPosition("APPL").orElse(null));

        //   And a sell order for 20 shares of MSFT stock should have been executed
        verify(mockMarketDao).execute(sellOrder);
    }
<!-- .element style="font-size: 0.41em; width: 100%;" -->


I think we can still do better


Let's examine our assertions

    // Then I should have 80 shares of MSFT stock
    assertEquals(new Position("MSFT", 80.0),
            portfolio.getPosition("MSFT").orElse(null));
    
    //   And I should have 150 shares of APPL stock
    assertEquals(new Position("APPL", 150.0),
            portfolio.getPosition("APPL").orElse(null));

These assertions focus on the implementation rather than the observable consequences of the behavior we are testing
<!-- .element: class="fragment" data-fragment-index="1" -->


Why does our test need to care about dealing with Optional?

---

# What is a Matcher?


An object allowing 'match' rules to be defined declaratively


Hamcrest is not a test framework, but Matchers are very useful in tests

  * UI Validation   <!-- .element: class="fragment" data-fragment-index="1" -->
  * data filtering  <!-- .element: class="fragment" data-fragment-index="2" -->

---

# Why should I use Matchers?


Allows writing flexible tests without over-specifying expected behavior


Tests can be written in a sort of mini-DSL, which can help you test intended behavior rather than implementation

Note: This helps tests break less often when unimportant changes are made

---

## Compare
Typical Assertion

    assertEquals(new Position("MSFT", 80.0),
        portfolio.getPosition("MSFT").orElse(null));

Assertion Using Matcher
<!-- .element: class="fragment" data-fragment-index="1" -->

    assertThat(portfolio,
        hasPosition(new Position("MSFT", 80.0)));
<!-- .element: class="fragment" data-fragment-index="1" -->

Note:
* More legible, the second assertion reads almost like your QA Analyst is talking you through it
* Describes the observable effect, instead of the underlying structure
* Provides documentation on what code _should_ do, instead of how it does it
* Better failure messages
* Type safety


### Failure Messages
    assertTrue(portfolio.getPositions().contains(new Position("MSFT", 70.0)));
    assertFalse(portfolio.getPositions().contains(new Position("MSFT", 10.0)));
    
        java.lang.AssertionError
            at org.junit.Assert.fail(Assert.java:86)
            at ...
<!-- .element style="font-size: 0.5em; width: 100%;" -->


### Failure Messages
    assertEquals(new Position("MSFT", 70.0),
            portfolio.getPosition("MSFT").orElse(null));
    
        java.lang.AssertionError: 
            Expected :Position{symbol='MSFT', units=70.0}
            Actual   :Position{symbol='MSFT', units=80.0}


### Failure Messages
    assertThat(portfolio, hasPosition(new Position("MSFT", 80.0)));
    
        java.lang.AssertionError: 
        Expected: Portfolio should contain a Position{symbol='MSFT', units=80.0}
             but: Portfolio contains Position{symbol='MSFT', units=70.0}
        Position{symbol='APPL', units=150.0}
<!-- .element style="font-size: 0.5em; width: 100%;" -->


### Type Safety
    assertEquals("abc", 123); //compiles, but fails

    assertThat(123, is("abc")); //does not compile

---

Mockito also has Matchers


Don't confuse them


Mockito Matchers are used for parameters in a stubbed method call

    when(environmentsDao.loadEnvironment(any(String.class)))
        .thenReturn(environment);


But you can use the *argThat* method in Mockito to pass a Hamcrest Matcher when you really need to

    when(environmentsDao.loadEnvironment(
            argThat(containsString("fake-environment"))))
        .thenReturn(environment);

---

## Improved Test v4
    @Test
    public void userTradesStocks_v4() throws MarketClosedException {
        Trade sellOrder = new SellOrder("MSFT", 20.0);

        // Use a mock MarketDao to avoid real trades being executed from our test!
        MarketDao mockMarketDao = mock(MarketDao.class);
        when(mockMarketDao.execute(sellOrder)).thenReturn(new Transaction(sellOrder));

        // Use a mock TradeClock so we can control its behavior and run our test at
        // any time of day.
        TradeClock mockClock = mock(TradeClock.class);
        when(mockClock.isMarketOpen()).thenReturn(true);

        Portfolio portfolio = new Portfolio.Builder()
                // Given I have 100 shares of MSFT stock
                .withPosition(new Position("MSFT", 100.0))
                //   And I have 150 shares of APPL stock
                .withPosition(new Position("APPL", 150.0))
                //   And the time is before close of trading
                .withTradeClock(mockClock)
                .withMarketDao(mockMarketDao)
                .build();
        
        // When I ask to sell 20 shares of MSFT stock
        portfolio.trade(sellOrder);

        // Then I should have 80 shares of MSFT stock
        assertThat(portfolio, hasPosition(new Position("MSFT", 80.0)));
        //   And I should have 150 shares of APPL stock
        assertThat(portfolio, hasPosition(new Position("APPL", 150.0)));

        //   And a sell order for 20 shares of MSFT stock should have been executed
        verify(mockMarketDao).execute(sellOrder);
    }
<!-- .element style="font-size: 0.47em; width: 100%;" -->

Now this is a great test!
<!-- .element: class="fragment" data-fragment-index="1" -->

---

# The Matcher Interface


## Matcher
    public interface Matcher<T> extends SelfDescribing {
        /** performs the matching */
        boolean matches(Object item);
        
        /** describe what was actually found */
        void describeMismatch(
            Object item,
            Description mismatchDescription);
    }
    
    public interface SelfDescribing {
        /** describe what is expected */
        void describeTo(Description description);
    }


You usually want to extend TypeSafeMatcher, however


## Type Safe Matcher
    public abstract class TypeSafeMatcher<T>
        extends BaseMatcher<T> {
        
        protected abstract boolean matchesSafely(T item);
        
        protected void describeMismatchSafely(
            T item,
            Description mismatchDescription) {
    }
    
    public interface SelfDescribing {
        /** describe what is expected */
        void describeTo(Description description);
    }


## Portfolio Matcher
    public class PortfolioMatcher {

    public static TypeSafeMatcher<Portfolio> hasPosition(
        Position expectedPosition) {
        
        return new TypeSafeMatcher<Portfolio>() { ... };
    }

Note: PortfolioMatcher could have many matchers available through convenient static methods.


Using the static import

    import static com.github.quincy.PortfolioMatcher.hasPosition;
    
    //...
    
    assertThat(portfolio,
        hasPosition(new Position("MSFT", 80.0)));


## describe To
    @Override
    public void describeTo(Description description) {
        // describe what is expected
        description.appendText("Portfolio should contain a ")
                .appendValue(expectedPosition);
    }


## describe Mismatch Safely
    @Override
    protected void describeMismatchSafely(
        Portfolio portfolio,
        Description mismatchDescription) {
        
        // describe what was actually found
        mismatchDescription.appendText("Portfolio contains ");
        mismatchDescription.appendText(portfolio.getPositions()
            .stream()
            .map(Position::toString)
            .collect(joining("\n")));
    }


## matches Safely
    @Override
    protected boolean matchesSafely(Portfolio portfolio) {
        Optional<Position> maybePosition
            = portfolio.getPosition(expectedPosition.getSymbol());
            
            return maybePosition.map(gotPosition 
                -> gotPosition.equals(expectedPosition))
                    .orElse(false);
        }
<!-- .element style="font-size: 0.52em;" -->


That's all there is to it


Hold your horses...

All you did was move the implementation specific code into the matcher!

---

## Built-in Matchers
There are lots of great Matchers that ship with Hamcrest


Object Matchers
* equalTo 
* hasToString
* instanceOf, isCompatibleType
* notNullValue, nullValue
* sameInstance


Bean Matchers
* hasProperty


Collection Matchers
* array
* hasEntry, hasKey, hasValue
* hasItem, hasItems
* hasItemInArray
* contains
* containsInAnyOrder


Number Matchers
* closeTo
* greaterThan
* greaterThanOrEqualTo
* lessThan
* lessThanOrEqualTo


String Matchers
* equalToIgnoringCase
* equalToIgnoringWhiteSpace
* containsString, endsWith, startsWith
* matchesPattern


Logical Matchers
* allOf
* anyOf
* not


This is only the beginning of what Hamcrest has to offer

Plus, there are lots of third party Matchers available too!


## XmlAssertion
    @Test
    public void createBranchBuildJobUsesCorrectCredentialsID() {
        // test setup...

        String gotXML = client.createBuildJobXML(params);

        XmlAssertion.assertThat(gotXML)
            .node("maven2-moduleset")
            .node("scm")
            .node("locations")
            .node("hudson.scm.SubversionSCM_-ModuleLocation")
            .node("credentialsId")
            .matches("devbuild");
    }
<!-- .element style="font-size: 0.47em;" -->

https://github.com/marcingrzejszczak/xmlassert


## hamcrest-json
    assertThat("{\"age\":43, \"friend_ids\":[16, 52, 23]}",
            sameJSONAs("{\"friend_ids\":[52, 23, 16]}")
                .allowingExtraUnexpectedFields()
                .allowingAnyArrayOrdering());

https://github.com/hertzsprung/hamcrest-json


## Webdriver Matchers
    assertThat(title, className("big-title");
    assertThat(button, value("Start Search"));
    assertThat(button, value(containsString("Search")));

https://github.com/yandex-qatools/matchers-java/tree/master/webdriver-matchers

---

# Review
* The goals of testing
* A brief discussion on Test Doubles
* Given, When, Then testing
* Improving a typical test
* What is a Matcher?
* Implementing the Matcher interface
* Existing Matchers


Matchers will not solve all of your testing problems


But they will help you distance your testing from the implementation


If your test is difficult to write, you probably need to address a design problem

---

# Questions?

The slides and all of the code are available at

[https://github.com/quincy/matchers-presentation](https://github.com/quincy/matchers-presentation)

![matchers=hamcrest](res/anagram.gif)
