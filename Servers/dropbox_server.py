import pytz
from flask import Flask, request
from werkzeug.utils import secure_filename
import os
from datetime import date, datetime, timedelta
from PIL import Image as pil_image
import threading
import uuid
import smtplib
from email import encoders
from email.mime.base import MIMEBase
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import dropbox
from Servers.layout_creator import create_pdf

app = Flask(__name__)

# ======== constants =========

DROPBOX_REFRESH_TOKEN = "lho67W14VEoAAAAAAAAAAW7jjnPRUJOCJcZKtHATJ0CWUtTHB5_mSVWcq67XSyiK"
DROPBOX_APP_KEY = "5iuu3fxivtjwnhq"
DROPBOX_APP_SECRET = "szoau61b63uhrpl"
PDFS_ANS_EMAILS_DIR = 'pdfs_and_emails'

FROM_EMAIL = "everydaymoments2023@gmail.com"
EMAIL_PW = 'gvycstbwmqxqgyyj'

SENDING_HOUR = 20
SENDING_MINUTE = 55
DELAY_IN_DAYS = 0

# ========== remote functions =======

DATE_FORMAT = "%d-%m-%Y"


def save_user_config(email_address, pdf_file_path):
    dbx = dropbox.Dropbox(
        app_key=DROPBOX_APP_KEY, app_secret=DROPBOX_APP_SECRET, oauth2_refresh_token=DROPBOX_REFRESH_TOKEN)
    moment_id = uuid.uuid4()

    now = get_israel_time_now()
    sending_date = now + timedelta(days=DELAY_IN_DAYS)
    formatted_sending_date = sending_date.strftime(DATE_FORMAT)
    formatted_date = now.strftime("%d-%m-%Y")

    dbx_pdf_name = f"/{PDFS_ANS_EMAILS_DIR}/{formatted_sending_date}/{moment_id}/everyday-moment-{formatted_date}.pdf"
    dbx_email_name = f"/{PDFS_ANS_EMAILS_DIR}/{formatted_sending_date}/{moment_id}/metadata.txt"

    bin_metadata = email_address.encode('utf-8')
    dbx.files_upload(bin_metadata, dbx_email_name, mode=dropbox.files.WriteMode.overwrite)

    with open(pdf_file_path, 'rb') as f:
        file_data = f.read()
        dbx.files_upload(file_data, dbx_pdf_name, mode=dropbox.files.WriteMode.overwrite)


# ========== helpers =======


def get_israel_time_now():
    return datetime.now(pytz.timezone('Israel'))


# ========== main functions =======


@app.route('/data', methods=['POST'])
def receive_data():
    text_data = request.form['text']
    image = request.files['image']
    email_address = request.form['email']

    print(email_address, flush=True)
    print(text_data, flush=True)

    new_uuid = uuid.uuid4()
    filename = secure_filename(image.filename)
    thread_temp_dir = os.path.join("/tmp/email_server_temp", str(new_uuid))
    if not os.path.exists(thread_temp_dir):
        os.makedirs(thread_temp_dir)

    image_path = os.path.join(thread_temp_dir, filename)
    image.save(image_path)

    pimage = pil_image.open(image_path)
    pimage = pimage.resize((pimage.width // 4, pimage.height // 4), pil_image.LANCZOS)
    pimage.save(image_path)

    thread = threading.Thread(target=create_and_upload_moment,
                              args=(text_data, image_path, email_address, thread_temp_dir))
    thread.daemon = True
    thread.start()

    return "success"


def create_and_upload_moment(text_data, image_path, email_address, thread_temp_dir):
    print("here create_and_upload_moment", flush=True)
    pdf_file_path = os.path.join(thread_temp_dir, "printpdf.pdf")
    create_pdf(image_path, pdf_file_path, text_data, thread_temp_dir)
    save_user_config(email_address, pdf_file_path)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=10000, threaded=True)
