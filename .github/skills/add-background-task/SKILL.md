---
name: add-background-task
description: Run a long admin backend operation (publishing, refreshing galleries, data sets) as a durable background task with per-item progress. Use when an endpoint would block or must show progress in the task tray.
---

# Background tasks in vempain-admin-backend

The facility is `vempain-common-core` (`fi.poltsi.vempain.common.task`); this service hosts it with
`fi.poltsi.vempain.admin.task.AdminTaskCommandExecutor` and `fi.poltsi.vempain.admin.api.TaskTypeEnum`.

## Steps

1. Add the type to `TaskTypeEnum` (api module) and the `case` to `AdminTaskCommandExecutor.execute` (payload JSON -> service call through
   `applicationContext.getBean(...)`).
2. Service pair, as in `PublishService`:
    - `xxxAsTask(...)`: authorize synchronously (`canPublishPage`/`canPublishGallery`, skip unauthorized items and count them as
      `skipped`), compute the total at submit time, `taskRunner.submitDurable(type.name(), title, totalSteps, payload, task -> xxxNow(..., task))`,
      return `progress.toAcceptedResponse()`.
    - `xxxNow(..., TaskProgress progress)`: not transactional itself; each item runs through the transactional proxy
      (`publishServiceProxy().publishGalleryAsSystem(id, progress)`), `progress.checkpoint()` before an item, `advance`/`advanceFailed` per
      smallest unit, a thrown item marks its remaining expected steps failed (`failRemainingSteps`), result map `{"published","skipped"}`.
3. Count the smallest items: one step per file transferred to the site (`JschClient.transferFilesToSite(..., onFileTransferred)`), one
   for an empty gallery, the page itself plus its galleries' files for a page (`pagePublishSteps`/`galleryPublishSteps`).
4. Controller: immediate publish endpoints answer `202` with the task inside `PublishResponse.task` (or a bare `TaskAcceptedResponse`);
   scheduling still answers `200` without a task.
5. Admin frontend: `trackTask(task, {onFinished})` from `@vempain/vempain-common-frontend`, no blocking spinner.

## Rules

- Publishing has no compensations: a cancelled publish stops between items and leaves published ones in place. Never catch
  `TaskCancelledException`.
- Progress writes commit in their own transaction (`vempain-common` 1.0.4+), so reporting inside `@Transactional` bodies is correct.
- `*AsSystem` methods skip authorization and are reserved for task bodies and `PublishItemSchedule`; never call them from controllers.

## Tests

- `PublishServiceUTC` pattern: `new TaskRunner(new TaskProgressStore(), Runnable::run)` as a `@Spy`, `Mockito.spy(service)`, stub the
  `*AsSystem(id, progress)` overloads with `doAnswer` that advances the progress, assert `accepted.getTotalSteps()`, status, result,
  completed/failed steps. `AdminTaskCommandExecutorUTC` for dispatch, `TaskCTC` for the hosted `/tasks` API, `DataCTC` end to end.
