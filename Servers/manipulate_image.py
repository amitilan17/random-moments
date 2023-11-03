import os
import numpy as np
import cv2
import random
from filmgrainer import filmgrainer


def apply_channel_mixer(image, blue_intensity):
    # Split the image into its individual color channels
    blue, green, red = cv2.split(image)

    # Scale the blue channel by the desired blue intensity factor
    blue = np.clip(blue * blue_intensity, 0, 255).astype(np.uint8)

    # Merge the modified channels back into a single image
    result = cv2.merge((blue, green, red))

    return result


def soft_light_blend(original, adjusted):
    # Normalize the pixel values to the range [0, 1]
    original_norm = original.astype(np.float32) / 255.0
    adjusted_norm = adjusted.astype(np.float32) / 255.0

    # Apply the soft light blending formula
    result_norm = np.where(adjusted_norm <= 0.5, 2 * original_norm * adjusted_norm,
                           1 - 2 * (1 - original_norm) * (1 - adjusted_norm))

    # Scale the result back to the range [0, 255]
    result = (result_norm * 255).astype(np.uint8)

    return result


def apply_exposure_adjustment(image, exposure, offset, gamma):
    # Normalize the pixel values to the range [0, 1]
    normalized_image = image.astype(np.float32) / 255.0

    # Apply the exposure adjustment
    adjusted_image = normalized_image * (2.0 ** (exposure / 20.0))

    # Apply the offset adjustment
    adjusted_image = np.clip(adjusted_image + offset / 1, 0.35, 1)

    # Apply the gamma correction
    adjusted_image = np.power(adjusted_image, gamma)

    # Scale the result back to the range [0, 255]
    result = (adjusted_image * 255).astype(np.uint8)

    return result


def add_brightness(image, brightness):
    # Convert the image to float32 format for arithmetic operations
    image_float = image.astype(np.float32)

    # Add the brightness value to each pixel
    brightened_image = np.clip(image_float + brightness, 0, 255)

    # Convert the image back to uint8 format
    result = brightened_image.astype(np.uint8)

    return result


def reduce_saturation(image, reduction_factor):
    # Convert the image from BGR to HSV color space
    hsv_image = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)

    # Split the channels (Hue, Saturation, Value)
    h, s, v = cv2.split(hsv_image)

    # Reduce the saturation channel by the specified reduction factor
    s = np.clip(s * reduction_factor, 0, 255).astype(np.uint8)

    # Merge the modified channels back into the HSV image
    hsv_modified = cv2.merge([h, s, v])

    # Convert the modified image back to BGR color space
    modified_image = cv2.cvtColor(hsv_modified, cv2.COLOR_HSV2BGR)

    return modified_image


def apply_blur(image, blur_amount):
    blurred_image = cv2.blur(image, (blur_amount, blur_amount), 0)
    blurred_image2 = cv2.blur(blurred_image, (blur_amount, blur_amount), 0)
    blurred_image3 = cv2.blur(blurred_image2, (blur_amount, blur_amount), 0)

    return blurred_image3


def zoom_in_and_crop(image, zoom_factor):
    # Get the dimensions of the original image
    original_height, original_width = image.shape[:2]

    crop_width = original_width
    crop_height = original_height

    # Calculate the new dimensions after zooming in
    zoomed_width = int(original_width * zoom_factor)
    zoomed_height = int(original_height * zoom_factor)

    # Resize the image using the zoomed dimensions
    zoomed_image = cv2.resize(image, (zoomed_width, zoomed_height))

    # Calculate the maximum valid top-left corner position
    max_x = zoomed_width - crop_width
    max_y = zoomed_height - crop_height

    # Generate random x, y coordinates within the valid range
    x = random.randint(0, max_x)
    y = random.randint(0, max_y)

    # Crop the image to the desired dimensions
    cropped_image = zoomed_image[y:y + crop_height, x:x + crop_width]

    return cropped_image


def activate_film_grainer(file_in, file_out):
    gray_scale = False
    grain_power = 0.7
    highs = 0.7
    shadows = 0.7
    grain_sat = 0.5
    grain_type = 2
    scale = 1.0
    src_gamma = 1.0
    sharpen = 0
    seed = 1

    filmgrainer.process(file_in, scale, src_gamma,
                        grain_power, shadows, highs, grain_type,
                        grain_sat, gray_scale, sharpen, seed, file_out)


def manipulate_image(original_image_path, blur_amount=30):
    original_image = cv2.imread(original_image_path)

    adjusted_exposure = apply_exposure_adjustment(original_image, exposure=0.07, offset=0.0895, gamma=1.19)
    added_brightness = add_brightness(adjusted_exposure, brightness=25)
    adjusted_blue = apply_channel_mixer(added_brightness, blue_intensity=2.0)
    reduced_saturation = reduce_saturation(adjusted_blue, reduction_factor=0.5)
    blured = apply_blur(reduced_saturation, blur_amount=blur_amount)
    colored_image = zoom_in_and_crop(blured, zoom_factor=1.5)

    film_grainer_dir_name = os.path.dirname(original_image_path)
    film_grainer_in = os.path.join(film_grainer_dir_name, "in.jpg")
    film_grainer_out = os.path.join(film_grainer_dir_name, "out.jpg")
    cv2.imwrite(film_grainer_in, colored_image)
    activate_film_grainer(film_grainer_in, film_grainer_out)

    return film_grainer_out
