from flask import Flask, request
from werkzeug.utils import secure_filename
import os
from cleanedForGithub.layout_creator import create_pdf
from concurrent.futures import ThreadPoolExecutor
import threading
import uuid

app = Flask(__name__)

FILE_NAME_FOR_PRINTING = "printpdf.pdf"


@app.route('/data', methods=['POST'])
def receive_data():
    text_data = request.form['text']
    image = request.files['image']

    new_uuid = uuid.uuid4()
    filename = secure_filename(image.filename)
    thread_temp_dir = os.path.join("/tmp/print_server_temp", str(new_uuid))
    if not os.path.exists(thread_temp_dir):
        os.makedirs(thread_temp_dir)

    image_path = os.path.join(thread_temp_dir, filename)
    image.save(image_path)

    thread = threading.Thread(target=create_and_print, args=(text_data, image_path, thread_temp_dir))
    thread.start()

    return "success"


def create_and_print(text_data, image_path, thread_temp_dir):
    pdf_file_path = os.path.join(thread_temp_dir, FILE_NAME_FOR_PRINTING)
    create_pdf(image_path, pdf_file_path, text_data, thread_temp_dir)
    command = f"lp -o media=105x297mm.Fullbleed {pdf_file_path}"  # -o ColorModel=Gray
    os.system(command)


if __name__ == '__main__':
    t_pool = ThreadPoolExecutor(4)
    app.run(host='0.0.0.0', port=8000, threaded=True)
