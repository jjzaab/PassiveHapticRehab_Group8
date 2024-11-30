using UnityEngine;
using UnityEngine.UI;
using System.Collections.Generic;
using System.Collections;

[RequireComponent(typeof(Image))]
public class ImageAnimator : MonoBehaviour
{
    public List<Sprite> sprites = new List<Sprite>();
    public float frameDuration = 0.1f;
    public enum AnimationMode {
        ONCE,
        LOOP,
        YOYO
    }
    public AnimationMode mode = AnimationMode.ONCE;

    private Image image;
    private bool animate = true;
    private int spriteIndex = 0;
    private int direction = 0;  // 0 - forward, 1 - backward

    private void OnEnable( ) {
        // Do nothing if there are no images
        if (sprites.Count == 0)
            return;

        if (image == null)
            image = GetComponent<Image>();

        StartCoroutine(AnimateImage());
    }

    private IEnumerator AnimateImage() {
        // Loop through images as long as its desirable
        while (animate) {
            // Hold the current image for the set duration
            yield return new WaitForSeconds(frameDuration);
        
            // Increment the index
            switch (mode) {
                case AnimationMode.ONCE:
                    spriteIndex++;
                    if (spriteIndex == sprites.Count - 1)
                        animate = false;

                    break;

                case AnimationMode.LOOP:
                    spriteIndex = (spriteIndex + 1) % sprites.Count;
                    break;

                case AnimationMode.YOYO:
                    if (direction == 0)
                        spriteIndex++;
                    else
                        spriteIndex--;

                    if (spriteIndex == 0)
                        direction = 0;
                    else
                        direction = 1;

                    break;
            }


            // Set the new image
            image.sprite = sprites[spriteIndex];
        }

    }

}
