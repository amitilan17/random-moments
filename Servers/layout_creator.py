from reportlab.lib.enums import TA_JUSTIFY
import os
from pdfminer.high_level import extract_text
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.pagesizes import A6
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfgen import canvas
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Image, PageBreak, Flowable
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.pdfbase.ttfonts import TTFont
from bidi.algorithm import get_display
from reportlab.lib.units import cm, mm

from cleanedForGithub.email_server import get_israel_time_now
from manipulate_image import manipulate_image
from get_main_words import get_main_words_str
from memory_profiler import profile

EIGHTY_FONT_SIZE = 10
GRETA_FONT_SIZE = 9.5
LINE_SPACE_FACTOR = 1.3
GRETA_FONT_NAME = 'Greta'
EIGHTY_FONT_NAME = 'Eighty'
PARA_WIDTH_MM = 80
PARA_HORIZONTAL_MARGIN_MM = 12.5
PARA_VERTICAL_MARGIN_MM = 31.75


def get_eighty_font_style():
    style = getSampleStyleSheet()
    pdfmetrics.registerFont(TTFont(EIGHTY_FONT_NAME, '80-kb-Soft.ttf'))
    eighty_style = ParagraphStyle(
        'EightyStyle',
        parent=style['Normal'],
        fontName=EIGHTY_FONT_NAME,
        fontSize=EIGHTY_FONT_SIZE,
        textColor='red',
        alignment=1,  # center alignment
        spaceAfter=12,  # space after paragraph
    )
    return eighty_style


def get_greta_font_style():
    style = getSampleStyleSheet()
    pdfmetrics.registerFont(TTFont('Greta', 'greta.ttf'))
    greta_style = ParagraphStyle(
        'GretaStyle',
        parent=style['Normal'],
        fontName=GRETA_FONT_NAME,
        fontSize=GRETA_FONT_SIZE,
        textColor='black',
        alignment=TA_JUSTIFY,  # right alignment
        spaceAfter=12,  # space after paragraph
    )
    return greta_style


class JustifiedParagraphDrawer(Flowable):
    """
    Draws a justified, aligned to right paragraph.
    Only support in LTR text, meaning RTL text input should be already reversed.
    """
    def __init__(self, text_lines):
        Flowable.__init__(self)
        self.text_lines = text_lines

    def draw(self):
        """
        To achieve justified paragraph, each line of text set with custom char spacing to fill the required width.
        Last line calculated to be aligned to the right.
        """
        c = self.canv
        c.setFont(GRETA_FONT_NAME, GRETA_FONT_SIZE)
        desired_length = PARA_WIDTH_MM * mm
        for i, t in enumerate(self.text_lines[:-3]):
            char_space_width = calculate_char_spacing(t, GRETA_FONT_NAME, GRETA_FONT_SIZE, desired_length)
            text_object = c.beginText(PARA_HORIZONTAL_MARGIN_MM * mm,
                                      0 - (PARA_VERTICAL_MARGIN_MM * mm) - (i * GRETA_FONT_SIZE * LINE_SPACE_FACTOR))
            text_object.setCharSpace(char_space_width)
            text_object.textLine(t)
            c.drawText(text_object)

        final_t = self.text_lines[-3]
        text_width = c.stringWidth(final_t, GRETA_FONT_NAME, GRETA_FONT_SIZE)
        i = len(self.text_lines[:-3])
        text_object = c.beginText(PARA_HORIZONTAL_MARGIN_MM * mm + desired_length - text_width,
                                  0 - (PARA_VERTICAL_MARGIN_MM * mm) - (i * GRETA_FONT_SIZE * LINE_SPACE_FACTOR))
        text_object.setCharSpace(0)
        text_object.textLine(final_t)
        c.drawText(text_object)


class ImageWithTextDrawer(Flowable):
    """
    Draws an image with the given text with underline and a background of white rectangle.
    """

    def __init__(self, img_path, text, width=297.63, height=419.52):
        Flowable.__init__(self)
        self.img_path = img_path
        self.text = text
        self.width = width
        self.height = height

    def draw(self):
        img = Image(self.img_path, width=A6[0], height=A6[1])
        img.drawOn(self.canv, 0, 0)

        # Calculate the text width (assuming it is a single line of text)
        text_width = pdfmetrics.stringWidth(self.text, EIGHTY_FONT_NAME, EIGHTY_FONT_SIZE)
        text_height = EIGHTY_FONT_SIZE
        rect_width = text_width + 16
        rect_vertical_padding = 6
        rect_height = text_height + rect_vertical_padding * mm
        # Draw the rectangle
        self.canv.setFillColor('#ffffff')
        self.canv.setStrokeColor('#ffffff')
        rectangle_x = (self.width - rect_width) / 2
        rectangle_y = (self.height - rect_height) / 2
        self.canv.rect(rectangle_x, rectangle_y, rect_width, rect_height, fill=1)
        # Draw the Paragraph at the same position as the rectangle
        p = Paragraph(self.text, style=get_eighty_font_style())
        p.wrapOn(self.canv, rect_width, rect_height)
        text_y = rectangle_y + (rect_height - text_height) / 2
        p.drawOn(self.canv, rectangle_x, text_y)
        # Draw a line underneath the text
        self.canv.setStrokeColor('red')
        self.canv.setLineWidth(0.3)
        line_x = (self.width - text_width) / 2
        self.canv.line(line_x, text_y - 1 * mm, line_x + text_width, text_y - 1 * mm)


class DateLayoutDrawer(Flowable):
    """
    Draws a formatted date and time with centered line between them.
    """

    def __init__(self):
        Flowable.__init__(self)

    def draw(self):
        today = get_israel_time_now()
        formatted_date = today.strftime("%d-%m-%Y")
        day = Paragraph(formatted_date, style=get_eighty_font_style())
        day_width = pdfmetrics.stringWidth(formatted_date, EIGHTY_FONT_NAME, EIGHTY_FONT_SIZE)
        day_start_x = 29 * mm
        day_start_y = 0 - 10 * mm
        day.wrapOn(self.canv, day_width, EIGHTY_FONT_SIZE)
        day.drawOn(self.canv, day_start_x, day_start_y)

        current_time = get_israel_time_now()
        formatted_time = current_time.strftime("%H:%M:%S")
        hour = Paragraph(formatted_time, style=get_eighty_font_style())
        hour_width = pdfmetrics.stringWidth(formatted_time, EIGHTY_FONT_NAME, EIGHTY_FONT_SIZE)
        hour_start_x = 65 * mm
        hour.wrapOn(self.canv, hour_width, EIGHTY_FONT_SIZE)
        hour.drawOn(self.canv, hour_start_x, day_start_y)

        # Draw a line
        self.canv.setStrokeColor('red')
        self.canv.setLineWidth(0.3)
        line_y = day_start_y + (EIGHTY_FONT_SIZE / 2)
        self.canv.line(day_start_x + day_width + 2 * mm, line_y, hour_start_x - 2 * mm, line_y)


@profile
def create_pdf(image_file_path, pdf_file_path, text, thread_temp_dir):
    pg_size = (A6[0], A6[1] * 2)
    doc = SimpleDocTemplate(pdf_file_path, pagesize=pg_size, rightMargin=-6, leftMargin=-6, topMargin=-6,
                            bottomMargin=-6)

    story = list()

    words_str = get_main_words_str(text)
    reversed_words_str = words_str[::-1].encode('utf-8')

    manipulated_img_path = manipulate_image(image_file_path, blur_amount=20)

    img_with_text = ImageWithTextDrawer(manipulated_img_path, reversed_words_str)
    story.append(img_with_text)

    header = DateLayoutDrawer()
    story.append(header)

    story.append(JustifiedParagraphDrawer(get_reversed_text_lines(text, thread_temp_dir)))

    doc.build(story)


def get_reversed_text_lines(text, thread_temp_dir):
    """
    Returns a list of lines of text, each line is reversed, to imitate LTR text.
    To break the text to lines in correct places, we first draw a temp, reversed, justified paragraph
    with the required final width, and use it lines break to split.
    """
    greta_style = get_greta_font_style()
    justified_temp = os.path.join(thread_temp_dir, "/tmp/justified_temp.pdf")
    doc = SimpleDocTemplate(justified_temp, pagesize=(PARA_WIDTH_MM * mm, A6[1]), rightMargin=-6, leftMargin=-6,
                            topMargin=-6, bottomMargin=-6)

    para = Paragraph(text, style=greta_style)
    story = [para]
    doc.build(story)
    extracted_text = extract_text(justified_temp)
    extracted_text_lines = extracted_text.split("\n")
    extracted_text_lines = [get_display(line) for line in extracted_text_lines]

    return extracted_text_lines


def calculate_char_spacing(text, font_name, font_size, desired_width):
    """
    Calculate the required char space which with the given text, in given font and size, will take the desired_width.
    """
    c = canvas.Canvas("/tmp/temp.pdf")
    text_width = c.stringWidth(text, font_name, font_size)
    total_spacing = desired_width - text_width
    num_gaps = len(text) - 1
    extra_space = total_spacing / num_gaps

    return extra_space
