import random

question = ""
answer = 0


def make_task():
    global question
    global answer
    rand1 = random.randint(0, 50)
    rand2 = random.randint(0, 50)
    neg = random.randint(0, 1)

    if rand1 - rand2 < 0:
        rand1, rand2 = rand2, rand1

    if neg == 0:
        answer = rand1 + rand2
        question = str(rand1) + "+" + str(rand2) + "="
    else:
        answer = rand1 - rand2
        question = str(rand1) + "-" + str(rand2) + "="


def get_question():
    return question


def get_answer():
    return answer


def check_answer(ans, user_answer):
    return ans == user_answer
