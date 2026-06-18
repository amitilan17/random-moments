import openai
import json
import random
from openai import OpenAI
import os
import re

HEBREW_PREFIXES = ("ש" ,"ה", "ב", "ו", "ל")

def normalize_word(word):
    return re.sub(r'^[^\wא-ת]+|[^\wא-ת]+$', '', word)

def build_valid_words_set(valid_words):
    valid_set = set()

    for word in valid_words:
        clean_word = normalize_word(word)

        if not clean_word:
            continue

        # Original word is valid
        valid_set.add(clean_word)

        # Version without prefix is also valid
        if len(clean_word) > 1 and clean_word.startswith(HEBREW_PREFIXES):
            valid_set.add(clean_word[1:])

    return valid_set

def extract_words_gpt4(user_text, num_of_words_to_extract):
    client = OpenAI(api_key=os.environ.get("OPEN_AI_KEY"))

    instruction_pre = "מה הן שלוש המילים הכי חשובות בטקסט הבא: \n"
    instruction_post = 'תענה בדיוק בשלוש מילים בודדות שקיימות בטקסט, בפורמט json באופן הבא: {"first": "word1", "second": "word2", "third": "word3"}'

    if num_of_words_to_extract == 2:
        instruction_pre = "מה הן שתי המילים הכי חשובות בטקסט הבא: \n"
        instruction_post = 'תענה בדיוק בשתי מילים בודדות שקיימות בטקסט, בפורמט json באופן הבא: {"first": "word1", "second": "word2"}'
    elif num_of_words_to_extract == 1:
        instruction_pre = "מה המילה הכי חשובה בטקסט הבא: \n"
        instruction_post = 'תענה בדיוק במילה בודדת שקיימת בטקסט, בפורמט json באופן הבא: {"first": "word"}'

    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {
                "role": "user",
                "content": instruction_pre + user_text + instruction_post
            }
        ],
    )

    print(response.choices[0].message.content, flush=True)
    return response.choices[0].message.content


def replace_words_if_needed(chosen_words, valid_words):
    valid_set = build_valid_words_set(valid_words)

    for j in range(len(chosen_words)):
        chosen = normalize_word(chosen_words[j])

        if chosen not in valid_set:
            chosen_words[j] = random.choice(valid_words)

    return chosen_words


def get_main_words_str(text: str):
    """
    The product design includes three main words, chosen by chatGPT.
    The design requires string of three words with ; separator.
    """
    split_words = text.split(" ")
    num_of_words = len(split_words)

    if num_of_words < 5:
        num_of_needed_words = 1
    elif num_of_words < 12:
        num_of_needed_words = 2
    else:
        num_of_needed_words = 3

    extracted_words = ""
    try:
        extracted_words = extract_words_gpt4(text, num_of_needed_words)
        clean_json = extracted_words.strip()

        if clean_json.startswith("```json"):
            clean_json = clean_json[7:]

        if clean_json.endswith("```"):
            clean_json = clean_json[:-3]

        clean_json = clean_json.strip()

        json_words = json.loads(clean_json)
        words_list = list(json_words.values())
        words_list = replace_words_if_needed(words_list, split_words)
    except Exception as e:
        print(extracted_words)
        print(str(e))
        words_list = random.sample(split_words, num_of_needed_words)

    words_set = list(set(words_list))

    words_str = "; ".join(words_set) + ";"
    # exclude special chars
    words_str = words_str.replace(".", "")
    words_str = words_str.replace(",", "")
    words_str = words_str.replace("-", "")
    words_str = words_str.replace("?", "")
    words_str = words_str.replace("!", "")

    return words_str
