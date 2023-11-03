import openai
import json
import random


def extract_words_gpt4(user_text, num_of_words_to_extract):
    openai.api_key = "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"

    instruction_pre = "מה הן שלוש המילים הכי חשובות בטקסט הבא: \n"
    instruction_post = 'תענה בדיוק בשלוש מילים בודדות שקיימות בטקסט, בפורמט json באופן הבא: {"first": "word1", "second": "word2", "third": "word3"}'

    if num_of_words_to_extract == 2:
        instruction_pre = "מה הן שתי המילים הכי חשובות בטקסט הבא: \n"
        instruction_post = 'תענה בדיוק בשתי מילים בודדות שקיימות בטקסט, בפורמט json באופן הבא: {"first": "word1", "second": "word2"}'
    elif num_of_words_to_extract == 1:
        instruction_pre = "מה המילה הכי חשובה בטקסט הבא: \n"
        instruction_post = 'תענה בדיוק במילה בודדת שקיימת בטקסט, בפורמט json באופן הבא: {"first": "word"}'

    message = [{"role": "user", "content": instruction_pre + user_text + instruction_post}]
    response = openai.ChatCompletion.create(
        model="gpt-4-0613",
        messages=message,
        temperature=0.2,
        max_tokens=1000,
        frequency_penalty=0.0
    )
    return response["choices"][0]["message"].content


def replace_words_if_needed(chosen_words, valid_words):
    for j in range(len(chosen_words)):
        if chosen_words[j] not in valid_words:
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
        json_words = json.loads(extracted_words)
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
