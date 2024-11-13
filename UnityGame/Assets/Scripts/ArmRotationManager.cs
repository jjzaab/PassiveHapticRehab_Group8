using TMPro;
using Unity.VisualScripting;
using UnityEngine;
using UnityEngine.UIElements;

public class ArmRotationManager : MonoBehaviour
{
    /* SINGLTON */
    private static ArmRotationManager instance = null;
    public static ArmRotationManager Instance { get { return instance; } }

    /* OBJECT REFERENCES */
    [Header("Object References")]
    public TMP_Text angleText;          /* Reference to the text that will display the angle reading*/
    public Transform armPivot;          /* Reference to the pivot point that will rotate the farmer's arm */
    public RectTransform meterTargPivot;/* Reference to the pivot that will rotate the bend target */
    public RectTransform meterPivot;    /* Reference to the pivot point that will adjust the rotation bar */

    /* INPUT DATA */
    [Header("Input Data")]
    public bool readInputs = false;
    public float elbowAngle = 0f;                   /* The current elbow-angle reading of the player */
    public bool smoothInputStream;                  /* Should an averaging algorithm be applied to the input stream */

    /* DATA CONSTRAINTS*/
    // NOTE: THE PURPOSE OF THESE CONTRAINTS IS TO ADJUST THE DIFFICULT...THEY ALLOW THE MAXIMUM INPUT VALUE TO BE DECREASED WHICH WOULD HELP
    // IMPAIRED PLAYERS DO WELL IN THE GAME
    [Header("Data Constraints")]
    public float maxFarmerArmRotation = 55f;        /* In game-space, the maximum value the farmer's arm can rotate */
    public float elbowReading_minMapping = 90f;     /* What real-world value of the elbow angle will map to the minimum value for game mechanics */
    public float elbowReading_maxMapping = 180f;    /* What real-world value of the elbow angle will map to the maximum value for game mechanics */

    /* PRIVATE VARIABLES */ 
    private const int dataFrameSize = 50;
    private int dataPointsStored = 0;
    private float[] dataFrame = new float[dataFrameSize];
    private float minFarmerArmRotation = 0f;
    private float minProgressBarAngle = 101f; // hardcoded based on the progress bar graphic when this script was written
    private float maxProgressBarAngle = 0f;   // The angle should be 0 when the bar should be full, or the elbow is stretched to the maximum
    private float elbowStretch;

    private void Start( ) {
        if (instance == null )
            instance = this;

        if (readInputs)
            UpdateGameGraphicsFromElbowAngle();
    }

    private void Update( ) {
        // Do nothing if player input shouldn't be read
        if (!readInputs)
            return;

        // Get the computed angle of the elbow
        elbowAngle = GameManager.Instance.DataReceiver.getLeftElbowExtensionAngle();

        // Should the input stream be smoothed?
        // This would make for more enjoyable gameplay
        if (smoothInputStream) {
            // Fill the frame if its not at capacity
            if (dataPointsStored < dataFrameSize) {
                dataFrame[dataPointsStored] = elbowAngle;
                dataPointsStored++;

            // If it is at capacity, move the oldest measurement out, and add the new one at the end
            } else {
                for (int i = 0; i < dataFrameSize-1; i++) {
                    dataFrame[i] = dataFrame[i+1];
                }
                dataFrame[dataPointsStored-1] = elbowAngle;
            }

            // Compute the average of whatever is in the frame
            elbowAngle = 0;
            for (int i = 0; i < dataPointsStored; i++) {
                elbowAngle += dataFrame[i];
            }
            elbowAngle /= dataPointsStored;
        }
        Debug.Log(elbowAngle);
        UpdateGameGraphicsFromElbowAngle();
    }

    private void UpdateGameGraphicsFromElbowAngle() {
        // Ensure the input angle is bounded between the user-defined min and max
        elbowAngle = Mathf.Clamp(elbowAngle, elbowReading_minMapping, elbowReading_maxMapping);

        // Compute the players stretch factor from the min to max bound (between 0 and 1)
        elbowStretch = Mathf.InverseLerp(elbowReading_minMapping, elbowReading_maxMapping, elbowAngle);

        // Map the min elbow angle to 0 and use the amount of stretch and the maximum to interpret the game-scale elbow angle
        float interpretedAngle = Mathf.Lerp(0, elbowReading_maxMapping - elbowReading_minMapping, elbowStretch);

        // Display the elbow angle
        angleText.text = interpretedAngle.ToString("F2") + "°";

        // Update the meter
        meterPivot.rotation = Quaternion.AngleAxis(Mathf.Lerp(minProgressBarAngle, maxProgressBarAngle, elbowStretch), Vector3.forward);

        // Update the farmer's arm
        armPivot.rotation = Quaternion.AngleAxis(Mathf.Lerp(minFarmerArmRotation, maxFarmerArmRotation, elbowStretch), Vector3.forward);
    }


    public void SetBendTarget(float value, bool fromTop) {
        float val = fromTop ? 1 : -1;
        meterTargPivot.rotation = Quaternion.AngleAxis(val * Mathf.Lerp(minProgressBarAngle, maxProgressBarAngle, value), Vector3.forward);
    }

    public float GetElbowStretch() {
        return elbowStretch;
    }
}
