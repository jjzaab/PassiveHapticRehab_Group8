using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;


// This script is for the countdown timer logic
public class CountdownTime : MonoBehaviour
{
    public Image currImage; 
    public List<Sprite> images; 
    private int count = 0; 

    // Start is called before the first frame update
    void Start()
    {
        if (images.Count > 0 && currImage != null)
        {
            StartCoroutine(Countdown());
        }
    }

    // Method for the timer countdown 
    IEnumerator Countdown()
    {
        // Infinite loop to repeat the countdown for testing
        while (true) 
        {
            // Update the current image
            currImage.sprite = images[count]; 
            yield return new WaitForSeconds(3); 
            count++;

            // Resets to the first image if at the end of the list
            if (count >= images.Count) 
            {
                count = 0;
            }
        }
    }
}
