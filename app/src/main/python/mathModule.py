import random


def make_task(config):
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

    return question, answer

def check_answer(ans, user_answer):
    return ans == user_answer
