using UnityEngine;
using System.Collections;
using TMPro;

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
    public GameObject tutorialScreen;
    public TMP_Text startTimeText;
    public TMP_Text instructionText;
    public ImageAnimator extensionGif;
    public ImageAnimator flexionGif;

    /* GAME PARAMETERS */
    [Header("Game Parameters")]
    [Range(0, 15)] public int countdownTime = 5;

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

    private void Start( ) {
        SetupInstructionScreen(gameState);
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

        // Set the display text
        timeText.text = currentCountdownTime.ToString();

        // Start the countdown
        while (currentCountdownTime > 0) {
            yield return new WaitForSeconds(1f);

            // Decrement time
            currentCountdownTime--;

            // Update text
            timeText.text = currentCountdownTime.ToString();
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
            ArmRotationManager.Instance.SetBendTarget(0, true);
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

}