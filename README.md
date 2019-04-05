### HystrixCommand Circuit Open Test
We were trying to integrate with HystrixCommand and to test if the circuit opens and the HystrixCommandProperties affect the behavior.

### Behavior
The ForceCircuitBreakerCommandTest Junit test is used to 
1. To run of ForceCircuitCommand correctly so the circuit was closed
2. Then run of ForceCircuitCommand with failure making the circuit was open
3. Subsequent run of ForceCircuitCommand directly goes to fall back because the circuit was closed

### How to run
1. Import the project in IDE
2. Run the JUNIT4 class circuttest.ForceCircuitBreakerCommandTest.java
3. On console you will see "sleep interrupted" java.lang.InterruptedException which opens the circuit.
4. Test should end successfully.
