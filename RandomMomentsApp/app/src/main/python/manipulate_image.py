from PIL import Image
import numpy as np
import io
import os
import cv2
import random
import subprocess
from filmgrainer import filmgrainer

# ======= helpers =======
def encode_image_as_bytearray(image):
    # Create byte array output stream
    stream = io.BytesIO()

    # Save image to byte array output stream
    image.save(stream, format="PNG")

    # Convert output stream to byte array
    byte_array = stream.getvalue()

    return byte_array


def decode_byte_array_to_bitmap(byte_array):
    # Create a BytesIO object from the byte array
    byte_stream = io.BytesIO(byte_array)

    # Open the image using PIL
    image = Image.open(byte_stream)

    # Convert the image to bitmap (1-bit mode)
    bitmap_image = image.convert()

    return bitmap_image
# ======= helpers =======


def apply_gradient_map(bitmap_image):
    # Define start and end colors as RGB tuples
    start_color = (201, 204, 245)
    end_color = (255, 255, 255)

    # Load bitmap image as numpy array
    bitmap_image = np.array(bitmap_image.convert('L'))
    print(bitmap_image.shape)

    # Define start and end colors as numpy arrays
    start_color = np.array(start_color)
    end_color = np.array(end_color)

    # Compute gradient map as numpy array
    gradient_map = np.outer(np.linspace(0, 1, 256), end_color) + np.outer(np.linspace(1, 0, 256), start_color)
    gradient_map = gradient_map.astype(int)

    # Apply gradient map to image
    new_image = gradient_map[bitmap_image]

    # Convert numpy array back to bitmap image
    new_bitmap = Image.fromarray(new_image.astype(np.uint8), mode="RGB")

    return new_bitmap

# ======= WIP =======
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
    result_norm = np.where(adjusted_norm <= 0.5, 2 * original_norm * adjusted_norm, 1 - 2 * (1 - original_norm) * (1 - adjusted_norm))

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


def apply_film_grain(image, blur_amount, grain_amount):
    # Apply the blur effect
    blurred_image = cv2.GaussianBlur(image, (blur_amount, blur_amount), 0)

    # Generate random grain noise with the same size as the image
    noise = np.random.normal(0, grain_amount, image.shape).astype(np.uint8)

    # Add the grain noise to the blurred image
    grain_image = cv2.add(blurred_image, noise)

    return grain_image


def apply_blur(image, blur_amount):
    # Apply the blur effect
    blurred_image = cv2.GaussianBlur(image, (blur_amount, blur_amount), 0)
    blurred_image2 = cv2.GaussianBlur(blurred_image, (blur_amount, blur_amount), 0)

    return blurred_image2


def zoom_in_and_crop(image, zoom_factor, crop_x, crop_y):
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

# ======= WIP =======


# def manipulate_image(uri):
#     absolute_path = "/data/data/com.example.randommemories"+uri
#     print(absolute_path)
#     image = Image.open(absolute_path)
#     manipulated_bitmap_image = apply_gradient_map(image)
#     manipulated_bitmap_image.save(absolute_path, "PNG")

def run_shell_command(command):
    try:
        subprocess.check_output(command, shell=True)
        return
    except subprocess.CalledProcessError as e:
        print(f"Error executing command: {e}")
        return None

def manipulate_image(uri):
    absolute_path = "/data/data/com.example.randommemories"+uri
    print(absolute_path)
    image = cv2.imread(absolute_path)
    adjusted_exposure = add_brightness(apply_exposure_adjustment(image, 0.07, 0.0895, 1.19), 25)

    # Apply the channel mixer adjustment with a blue intensity of 2.0 (200% increase)
    adjusted_blue = apply_channel_mixer(adjusted_exposure, 2.0)

    final = zoom_in_and_crop(apply_blur(reduce_saturation(adjusted_blue, 0.5), 29), zoom_factor=2, crop_x=900, crop_y=1800)

    cv2.imwrite("/data/data/com.example.randommemories/cache/temp.jpg",final)
#     command = f"filmgrainer --type 2 --sat 0.5 --power 0.8,0.5,0.5 -o {absolute_path} /data/data/com.example.randommemories/cache/temp.jpg"
#     run_shell_command(command)

    file_in = "/data/data/com.example.randommemories/cache/temp.jpg"
    gray_scale=False
    grain_power=0.8
    highs=0.5
    shadows=0.5
    grain_sat=0.5
    grain_type=2
    scale=1.0
    src_gamma=1.0
    sharpen=0
    seed=1

    file_out = "/data/data/com.example.randommemories/cache/out.jpg"
#     file_out = absolute_path
    filmgrainer.process(file_in, scale, src_gamma,
                grain_power, shadows, highs, grain_type,
                grain_sat, gray_scale, sharpen, seed, file_out)
#     os.remove("temp.jpg")