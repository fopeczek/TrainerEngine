import random

question = ""
answer = 0


def make_task():
    global question
    global answer
    isok = False
    while not isok:
        rand1 = random.randint(1, 20)
        rand2 = random.randint(1, 20)
        answer = rand1 * rand2
        isok = answer <= 20

    question = f"{rand1}*{rand2}="

def get_question():
    return question


def get_answer():
    return answer


def check_answer(ans, user_answer):
    return ans == user_answer
