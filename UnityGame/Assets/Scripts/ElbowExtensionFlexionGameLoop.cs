using UnityEngine;
using System.Collections;
using TMPro;
using UnityEngine.UI;

public class ElbowExtensionFlexionGameLoop : MonoBehaviour {
    /* GAME STATE */
    public enum GameState {
        EXTENSION_TUTORIAL_SCREEN,
        EXTENSION_TUTORIAL_DEMO,
        FLEXION_TUTORIAL_SCREEN,
        FLEXION_TUTORIAL_DEMO,
        ROUND_START_SCREEN,
        ROUND_PLAY
    }
    public GameState gameState = GameState.EXTENSION_TUTORIAL_SCREEN;

    /* OBJECT REFERENCES */
    [Header("Object References")]
    // Carrot picking
    [Space(10)]
    public TMP_Text gameTimer;
    public Rigidbody2D carrotObject;

    // Instruction menu
    [Space(10)]
    public GameObject tutorialScreen;
    public TMP_Text startTimeText;
    public TMP_Text instructionText;
    public ImageAnimator extensionGif;
    public ImageAnimator flexionGif;


    // Score screen
    [Space(10)]
    public Image scoreScreen;
    public RectTransform scoreContentPanel;
    public TMP_Text scoreText;
    public TMP_Text nextRoundButtonText;
    public Image round1Circle;
    public Image round2Circle;
    public Image round3Circle;

    /* GAME PARAMETERS */
    [Header("Game Parameters")]
    [Range(0, 15)] public int countdownTime = 5;
    public int roundTime; // in seconds
    public Vector3 carrotPosition;
    public float maxCarrotHeight;
    public float carrotPickForce = 3;

    [Space(10)]
    // NOTE: These are all percentages so the targets can adaptively be scaled with difficulties
    // set in the ArmRotationManager
    [Range(0, 1)] public float extensionTutorialTarget = 0.75f;
    [Range(0, 1)] public float flexiontutorialTarget = 0.1f;
    [Range(0, 1)] public float carrotGrabTarget = 0.85f;
    [Range(0, 1)] public float carrotPullTarget = 0.05f;

    [Space(10)]
    public string extensionInstructionText;
    public string flexionInstructionText;
    public string roundStartText;

    private int currentCountdownTime;

    private int grabbedCarrot = 1; // 0 - can't grab, 1 - can grab, 2 - grabbed already....this is so the player can't grab a carrot that is spawning in
    private int activeCarrot = 0;
    private int[] carrotScore = new int[3];
    private int currentRound = 0;

    private void Start( ) {
        // Handle the current state of the game
        switch (gameState) {
            case GameState.EXTENSION_TUTORIAL_SCREEN:
                SetupInstructionScreen(gameState);
                break;

            case GameState.EXTENSION_TUTORIAL_DEMO:
                SetupGameplay(gameState);
                break;

            case GameState.FLEXION_TUTORIAL_SCREEN:
                SetupInstructionScreen(gameState);
                break;

            case GameState.FLEXION_TUTORIAL_DEMO:
                SetupGameplay(gameState);
                break;

            case GameState.ROUND_START_SCREEN:
                SetupInstructionScreen(gameState);
                break;

            case GameState.ROUND_PLAY:
                SetupGameplay(gameState);
                break;

        }
    }

    private void Update( ) {
        // Handle the current state of the game
        switch (gameState) {
            case GameState.EXTENSION_TUTORIAL_SCREEN:
                HandleInstructionScreen(GameState.EXTENSION_TUTORIAL_DEMO);
                break;

            case GameState.EXTENSION_TUTORIAL_DEMO:
                HandleDemoState(extensionTutorialTarget, ArmRotationManager.Instance.GetElbowStretch(), GameState.FLEXION_TUTORIAL_SCREEN);
                break;

            case GameState.FLEXION_TUTORIAL_SCREEN:
                HandleInstructionScreen(GameState.FLEXION_TUTORIAL_DEMO);
                break;

            case GameState.FLEXION_TUTORIAL_DEMO:
                HandleDemoState(1 - flexiontutorialTarget, 1 - ArmRotationManager.Instance.GetElbowStretch(), GameState.ROUND_START_SCREEN);
                break;

            case GameState.ROUND_START_SCREEN:
                HandleInstructionScreen(GameState.ROUND_PLAY);
                break;

            case GameState.ROUND_PLAY:
                HandleCarrotPicking();
                break;
        
        }
    }

    /*====================================================================
    SetupInstructionScreen: Populates the instruction screen UI with the
    appropriate data, according to the game state
    =====================================================================*/
    private void SetupInstructionScreen(GameState state) {
        // Turn on the screen
        tutorialScreen.SetActive(true);

        // Populate all of the text accordingly
        // Display a gif if neccesary
        if (state == GameState.EXTENSION_TUTORIAL_SCREEN) {
            instructionText.text = extensionInstructionText;
            extensionGif.gameObject.SetActive(true);
            flexionGif.gameObject.SetActive(false);

        }
        else if (state == GameState.FLEXION_TUTORIAL_SCREEN) {
            instructionText.text = flexionInstructionText;
            extensionGif.gameObject.SetActive(false);
            flexionGif.gameObject.SetActive(true);

        }
        else if (state == GameState.ROUND_START_SCREEN) {
            instructionText.text = roundStartText;
            extensionGif.gameObject.SetActive(false);
            flexionGif.gameObject.SetActive(false);

        }

        // Handle the countdown for the instruction menu before moving to a play state
        StartCoroutine(Countdown(countdownTime, startTimeText));
    }

    /*====================================================================
    Countdown: Countdown the timer for the current instruction
    screen.
    =====================================================================*/
    private IEnumerator Countdown(int time, TMP_Text timeText) {
        // Set internal counter to the countdown time
        currentCountdownTime = time;

        int minutes = time / 60;
        int seconds = time - (minutes * 60);

        // Set the display text
        if (minutes <= 0) timeText.text = seconds.ToString();
        else timeText.text = minutes.ToString() + ":" + seconds.ToString();
        

        // Start the countdown
        while (currentCountdownTime > 0) {
            yield return new WaitForSeconds(1f);

            // Decrement time
            currentCountdownTime--;

            minutes = currentCountdownTime / 60;
            seconds = currentCountdownTime - (minutes * 60);

            // Set the display text
            if (minutes <= 0) timeText.text = seconds.ToString();
            else timeText.text = minutes.ToString() + ":" + seconds.ToString();
        }
    }


    /*====================================================================
    HandleInstructionScreen: An instruction screen will essentially just
    wait for the countdown to expire and then move to the next state. The
    instruction text is set outside of this method.
    =====================================================================*/
    private void HandleInstructionScreen(GameState nextState) {
        // Still counting down
        if (currentCountdownTime > 0)
            return;

        // Set the state to the next one
        gameState = nextState;

        // Hide the instruction screen
        tutorialScreen.SetActive(false);

        // Setup the gameplay interface for the next state
        // NOTE: this assumes that a play state always follows an instruction state
        SetupGameplay(gameState);
    }


    /*====================================================================
    SetupGameplay: Sets up the game so the player can do what is necessary
    =====================================================================*/
    private void SetupGameplay(GameState state) {
        // The game will now read inputs
        ArmRotationManager.Instance.readInputs = true;

        // This will cover a region on the meter red,
        // this region is where the player needs to reach to progress to the next state
        if (state == GameState.EXTENSION_TUTORIAL_DEMO) {
            ArmRotationManager.Instance.SetBendTarget(1 - extensionTutorialTarget, false);

        } else if (state == GameState.FLEXION_TUTORIAL_DEMO) {
            ArmRotationManager.Instance.SetBendTarget(flexiontutorialTarget, true);

        } else if (state == GameState.ROUND_PLAY) {
            ArmRotationManager.Instance.SetBendTarget(1 - carrotGrabTarget, false);
            ArmRotationManager.Instance.readInputs = true;
            grabbedCarrot = 1;
            StartCoroutine(Countdown(roundTime, gameTimer));
        }
    }

    /*====================================================================
    HandleDemoState: Logic to walk the player through the demo states
    =====================================================================*/
    private void HandleDemoState(float target, float input, GameState nextState) {
        // Do nothing if the player hasn't reached the desired bend factor
        if (input < target)
            return;

        // Move to the next state, which will be an instruction screen
        SetupInstructionScreen(nextState);

        // Set the game state
        gameState = nextState;

        // Turn off inputs
        ArmRotationManager.Instance.readInputs = false;
    }

    /*====================================================================
    HandleCarrotPicking: Track user gameplay to grab and pull carrots.
    Track how many carrots the player picked
    =====================================================================*/
    private void HandleCarrotPicking() {
        // If the player has a carrot grabbed, poll their elbow angle
        // until they pull it out of the groundd
        if (grabbedCarrot == 2) {
            if ((1 - ArmRotationManager.Instance.GetElbowStretch()) > (1 - carrotPullTarget)) {
                grabbedCarrot = 0;
                ArmRotationManager.Instance.SetBendTarget(1 - carrotGrabTarget, false);
                carrotObject.gravityScale = 1;
                carrotObject.velocity = new Vector2(-15, 1);
                carrotScore[currentRound]++;
                StartCoroutine(SpawnCarrot());
            }
            carrotObject.MovePosition(new Vector2(carrotPosition.x, Mathf.Lerp(maxCarrotHeight, carrotPosition.y, ArmRotationManager.Instance.GetElbowStretch())));

        } else if (grabbedCarrot == 1) {
            if (ArmRotationManager.Instance.GetElbowStretch() > carrotGrabTarget) {
                grabbedCarrot = 2;
                ArmRotationManager.Instance.SetBendTarget(carrotPullTarget, true);
            }
        }

        // This means the round ends
        if (currentCountdownTime == 0) {
            currentCountdownTime = -1;
            grabbedCarrot = 0;
            StartCoroutine(ToggleScoreScreen(true));
        }
    }

    /*====================================================================
    SpawnCarrot: Doesnt actually spawn a game object, but uses the same
    carrot object over and over after it leaves the screen
    =====================================================================*/
    private IEnumerator SpawnCarrot() {
        // Scale it down
        int steps = 20;
        float tweenTime = 0.5f;
        float waitTime = tweenTime / steps;
        float stepSize = 1f / steps;
        Vector3 scaleChange = Vector3.one * stepSize;
        for (int i = 0; i < steps; i++) {
            yield return new WaitForSeconds(waitTime);
            carrotObject.transform.localScale -= scaleChange;
        }

        // Stop the carrot
        carrotObject.gravityScale = 0;
        carrotObject.velocity = Vector2.zero;

        // Shrink the object
        carrotObject.transform.localScale = Vector3.zero;

        // Move it to the optimal position
        carrotObject.MovePosition(carrotPosition);

        // Scale it up
        for (int i = 0; i < steps; i++) {
            yield return new WaitForSeconds(waitTime);
            carrotObject.transform.localScale += scaleChange;
        }

        // At the end of the scaling loop, allow the carrot to be grabbed
        grabbedCarrot = 1;
    }

    /*====================================================================
    ToggleScoreScreen: Turn on or off the score screen
    =====================================================================*/
    private IEnumerator ToggleScoreScreen(bool status) {
        // Scale it down
        int steps = 20;
        float tweenTime = 0.2f;
        float waitTime = tweenTime / steps;
        float stepSize = 1.75f / steps;
        float stepSize2 = .95f / steps;
        Vector3 scaleChange = Vector3.one * stepSize;

        // If turning on the screen, fade background and scale up the content
        if (status) {
            // Turn off player control
            ArmRotationManager.Instance.readInputs = false;

            // Display the score
            scoreText.text = carrotScore[currentRound].ToString();

            // Update the rounds, currentRound uses zero-based indexing so 1 is actually round 2
            // NOTE: there is probably a cleaner way to do this, but for now it is a bit brute forced
            // for round 3 to not use the button timer
            if (currentRound == 1) {
                round1Circle.color = new Color(0.8117648f, 0.8117648f, 0.8117648f);
                round2Circle.color = new Color(0.3568628f, 0.2392157f, 0.2235294f);
                round3Circle.color = new Color(0.8117648f, 0.8117648f, 0.8117648f);
                StartCoroutine(ButtonWaitTime(3));

            } else if (currentRound == 2) {
                round1Circle.color = new Color(0.8117648f, 0.8117648f, 0.8117648f);
                round2Circle.color = new Color(0.8117648f, 0.8117648f, 0.8117648f);
                round3Circle.color = new Color(0.3568628f, 0.2392157f, 0.2235294f);
                nextRoundButtonText.text = "View Results";
            } else {
                StartCoroutine(ButtonWaitTime(3));
            }
                

            for (int i = 0; i < steps; i++) {
                yield return new WaitForSeconds(waitTime);
                scoreContentPanel.localScale += scaleChange;
                scoreScreen.color = new Color(0, 0, 0, i * stepSize2); 
            }

        // If not, fade out the background and shrink the content
        } else {

            for (int i = steps - 1; i >= 0; i--) {
                yield return new WaitForSeconds(waitTime);
                scoreContentPanel.localScale -= scaleChange;
                scoreScreen.color = new Color(0, 0, 0, i * stepSize2);

            }
        }
    }

    /*====================================================================
    ButtonWaitTime: This is a timer specifically for the next round button
    that will progress the game automatically
    =====================================================================*/
    private IEnumerator ButtonWaitTime(int time) {
        int nextRound = currentRound + 2;
        nextRoundButtonText.text = "Jump to Round " + nextRound.ToString() + " (" + time.ToString() + "s...)";

        for (int i = time - 1; i >= 0; i--) {
            yield return new WaitForSeconds(1);
            nextRoundButtonText.text = "Jump to Round " + nextRound.ToString() + " (" + i.ToString() + "s...)";
        }

        // When this timer is up, go to the next round
        // ONLY, if still in the ROUND_PLAY state
        if (gameState == GameState.ROUND_PLAY)
            GoToNextRound();
    }

    /*====================================================================
    GoToNextRound: Go to the next round. Called from a button or auto-
    matically when a timer expires
    =====================================================================*/
    public void GoToNextRound() {
        // TODO: If the current round is the last round (round 3), then do special things
        // to show the results
        if (currentRound == 2) {

            return;
        }

        // Turn off score screen
        StartCoroutine(ToggleScoreScreen(false));

        // Reset data as needed
        SetupGameplay(gameState);

        // Increment the round
        currentRound++;
    }

}