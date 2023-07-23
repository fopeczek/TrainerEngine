import random

def make_task(config):
    isok = False
    while not isok:
        rand1 = random.randint(0, config.get(0))
        rand2 = random.randint(0, config.get(0))
        answer = rand1 * rand2
        isok = answer <= config.get(0)

    question = f"{rand1}*{rand2}="

    return question, answer

def check_answer(ans, user_answer):
    return ans == user_answer
