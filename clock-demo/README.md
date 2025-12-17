# Demo application to show how we might use a ClockService in our applications

The goal of the clock service is to allow easy inject of date / time into our applications

This makes testing easier, because we can can easily assert the outcome of tests

We use a ClockService to encapsulate the Clock methods with a simple single now() method
We could of course directly inject the Clock and mock the clock rather than the ClockService

See example integration tests

ClockMockIntegrationTest
... override the default Clock() with a MockitoBean Clock and thus we can set the mock date time that will be returned
( Thus we can easily test the outcomes because we know exactly what the clock will return )


ClockRealIntegrationTest
... uses the default Clock() that is configured in ClockConfig and thus returns the current date time
