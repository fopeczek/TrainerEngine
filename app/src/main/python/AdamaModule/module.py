from typing import Any, Optional
import random
import java
from dataclasses import dataclass

setting_descriptions = {
    "max_number": "Maximum number to be used in questions",
    "min_number": "Minimum number to be used in questions"}


@dataclass
class Settings:
    """
    Defines a dictionary of types required to store settings.
    """
    max_number: int = 99
    min_number: int = 1

    def __getitem__(self, item):  # Throws an error if setting does not exist
        return setting_descriptions[item]


def setting_list() -> dict[str, tuple[type, str]]:
    """
    Returns a list of settings that the module supports.
    Format is "setting_name -> (setting_type, setting_description)"
    """
    return {"max_number": (int, "Maximum number to be used in questions")}


def skill_list() -> dict[str, str]:
    """Returns a list of all skills. Skills are a special type of settings,
    that are used to randomize questions and that are in the form of triple-logic flags.
    """
    ans = {"twodigit": "Ability to add and subtract two-digit numbers",
           "subtract": "Ability to also subtract numbers",
           "overflow10": "Ability to add numbers where digit part overflows",
           "underflow10": "Ability to subtract numbers where digit part underflows",
           "negative": "Ability to use negative numbers"}
    return ans


@dataclass
class QuestionType:
    """
    Defines a dictionary of types required to store a question.
    """
    first_operand: int
    second_operand: int
    operator: str


judgement_descriptions = {
    "error_in_units": "Error in units part of the result",
    "error_in_tens": "Error in tens part of the result",
    "error_in_sign": "Wrong sign of the result",
    "error_in_operand": "Answer to addition whereas subtraction was asked or vice versa",
    "error_in_ordering": "Mistake in ordering of numbers in subtraction"
}


@dataclass
@dataclass
class JudgmentType:
    """
    Defines a dictionary of types required to store a judgment.
    """
    score_in_units: Optional[float] = 1.
    score_in_tens: float = 1.
    score_in_sign: Optional[float] = 1.
    score_in_operand: Optional[float] = 1.
    score_in_ordering: Optional[float] = 1.

    def __getitem__(self, item):  # Throws an error if setting does not exist
        return setting_descriptions[item]


@dataclass
class AnswerType:
    """
    Defines a dictionary of types required to store an answer.
    """
    answer: int

    def validation_rule(self, settings: Settings, positive_skills: list[str], negative_skills: list[str], field: str):
        assert field == "answer"

        return ""


def calc_score(judgement: JudgmentType, correct: AnswerType, skills: list[str]) -> dict[str, tuple[float, float]]:
    """
    Calculates the score of the given judgment.
    Returns number of points and total number of points
    """
    description = {}

    if "overflow10" in skills or "underflow10" in skills:
        answer_weight = 6
        mistake_weight = 3
    else:
        if "twodigit" in skills:
            answer_weight = 4
            mistake_weight = 2
        else:
            answer_weight = 2
            mistake_weight = 1

    description["units"] = ((answer_weight * (judgement.score_in_units > 0.5)), answer_weight)
    if judgement.score_in_tens is not None:
        description["tens"] = ((answer_weight * (judgement.score_in_tens > 0.5)), answer_weight)

    if judgement.score_in_sign is not None:
        description["sign"] = ((mistake_weight * (judgement.score_in_sign > 0.5)), mistake_weight)

    if judgement.score_in_ordering is not None:
        description["units"] = ((mistake_weight * (judgement.score_in_ordering > 0.5)), mistake_weight)

    if judgement.score_in_operand is not None:
        description["operand"] = ((mistake_weight * (judgement.score_in_operand > 0.5)), mistake_weight)

    weight = sum([x[1] for x in description.values()])
    sum_points = sum([x[0] for x in description.values()])
    description["overall"] = (sum_points, weight)

    return description


ModuleConfig = java.jclass("com.example.trainerengine.configs.ModuleConfig")
ConfigData = java.jclass("com.example.trainerengine.configs.ConfigData")

def make_task(
              config: ModuleConfig, positive_skills: list[str]=[], negative_skills: list[str]=[]) -> Optional[tuple[QuestionType, AnswerType, list[str]]]:
    """Returns a tuple representing the question, that was randomized according to the given skills,
    the correct answer and the list effective skills that are tested.
    or None if no question could be randomized.

    Assumes that no skill is both positive and negative.
    """

    assert not (set(list(positive_skills)) & set(
        negative_skills))  # It is Kotlin's responsibility to check for that, but here we check it again.

    flag_subtract = False
    if "subtract" in positive_skills:
        flag_subtract = True
    elif "subtract" not in negative_skills:
        # Randomize subtraction flag with 50% probability
        flag_subtract = random.random() < 0.5

    if "twodigit" not in positive_skills and "twodigit" not in negative_skills:
        randomize_digits = True
    else:
        randomize_digits = False

    max_attempts = 100
    max_number = config.getConfigData("Max value").getValue()
    min_number = config.getConfigData("Min value").getValue()

    if "twodigit" in negative_skills:
        max_number = min(9, max_number)
        min_number = max(0, min_number)
        twodigit = False
    else:
        twodigit = True

    if "twodigit" in positive_skills:
        min_number = max(10, min_number)
        max_number = max(min_number, max_number)

    for attempt in range(max_attempts):
        if randomize_digits:
            twodigit = random.random() < 0.5

        if twodigit:
            operand1 = random.randint(min_number, max_number)
            operand2 = random.randint(min_number, max_number)
        else:
            operand1 = random.randint(max(0, min_number), min(9, max_number))
            operand2 = random.randint(max(0, min_number), min(9, max_number))

        last_digit1 = operand1 % 10
        last_digit2 = operand2 % 10

        if flag_subtract:
            if "negative" in positive_skills:
                if operand1 > operand2:
                    operand1, operand2 = operand2, operand1
            elif "negative" in negative_skills:
                if operand1 < operand2:
                    operand1, operand2 = operand2, operand1

            if "underflow10" in positive_skills:
                if last_digit1 >= last_digit2:
                    continue
            elif "underflow10" in negative_skills:
                if last_digit1 < last_digit2:
                    continue

        else:  # Addition
            if "overflow10" in positive_skills:
                if last_digit1 + last_digit2 < 10:
                    continue
            elif "overflow10" in negative_skills:
                if last_digit1 + last_digit2 >= 10:
                    continue

        result = operand1 - operand2 if flag_subtract else operand1 + operand2
        if result > max_number or result < min_number:
            continue

        break
    else:
        return None  # No question could be randomized after max_attempts

    if flag_subtract:
        correct = operand1 - operand2
    else:
        correct = operand1 + operand2

    question = QuestionType(first_operand=operand1, second_operand=operand2, operator="-" if flag_subtract else "+")
    answer = AnswerType(answer=correct)

    skills = _infer_skills(question, answer)

    return (question, answer, skills)


def _infer_skills(question: QuestionType, answer: AnswerType) -> list[str]:
    operand1 = question.first_operand
    operand2 = question.second_operand
    correct = answer.answer
    flag_subtract = question.operator == "-"

    skills = []
    if -99 < operand1 < -10 or 10 < operand1 < 99 or -99 < operand2 < -10 or 10 < operand2 < 99:
        skills.append("twodigit")

    if flag_subtract:
        skills.append("subtract")

    last_digit1 = operand1 % 10
    last_digit2 = operand2 % 10

    if flag_subtract:
        if last_digit1 < last_digit2:
            skills.append("underflow10")
        if correct < 0 or operand1 < 0:
            skills.append("negative")
    else:
        if last_digit1 + last_digit2 >= 10:
            skills.append("overflow10")
        if operand1 < 0 or operand2 < 0:
            skills.append("negative")

    return skills


def check_answer(answer: Any, user_answer: Any) -> bool:
    """Returns True if the answer is correct, False otherwise."""
    ans = JudgmentType()

    correct_p = question.first_operand + question.second_operand
    correct_m = question.first_operand - question.second_operand
    correct_m2 = question.second_operand - question.first_operand

    if question.operator == "+":
        correct = correct_p
    else:
        correct = correct_m

    if -9 <= correct <= 9 and question.operator == "+":
        ans.score_in_tens = None
    if (question.operator == "-" and question.first_operand > question.second_operand) or question.operator == "+":
        ans.score_in_sign = None

    if question.operator == "+":
        ans.score_in_ordering = None

    correct_digit1 = correct % 10
    correct_digit2 = correct // 10

    ans_p_digit1 = answer.answer % 10
    ans_p_digit2 = answer.answer // 10

    # Simple case: check if the sign is correct
    if (answer.answer > 0) != (correct > 0):
        ans.score_in_sign = 0.

    # Check if the answer is correct but maybe to the wrong sign or ordering
    if question.operator == "+":
        if correct_p == answer.answer:
            return ans  # All is correct
        if correct_m == answer.answer:
            ans.score_in_operand = 0.  # The wrong sign, but otherwise ok
            return ans
    if question.operator == "-":
        if correct_m == answer.answer:
            return ans
        if correct_p == answer.answer:
            ans.score_in_operand = 0.
            return ans
        if correct_m2 == answer.answer:
            ans.score_in_ordering = 0.
            return ans

    # Now we know that the answer is generally wrong. Let's see if the
    # error is with units or tens.

    if correct_digit1 != ans_p_digit1:
        ans.score_in_units = 0.
    if correct_digit2 != ans_p_digit2 and ans.score_in_tens is not None:
        ans.score_in_tens = 0.

    return ans
